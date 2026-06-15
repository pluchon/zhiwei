package com.campus.system.service.repair;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.enums.AiSceneType;
import com.campus.system.common.enums.RepairStatus;
import com.campus.system.entity.RepairOrder;
import com.campus.system.mapper.RepairOrderMapper;
import com.campus.system.service.ai.AiCallResult;
import com.campus.system.service.ai.AiClientGateway;
import com.campus.system.service.ai.AiEmbeddingGateway;
import com.campus.system.service.ai.AiModelRouteService;
import com.campus.system.vo.DuplicateRepairCheckVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 重复报修识别服务
@Component
public class DuplicateRepairDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateRepairDetectionService.class);

    private static final Set<Integer> IN_PROGRESS = Set.of(RepairStatus.PENDING_DISPATCH.getCode(),
            RepairStatus.PENDING_ACCEPT.getCode(), RepairStatus.ACCEPTED.getCode(),
            RepairStatus.PROCESSING.getCode(), RepairStatus.PENDING_CONFIRM.getCode(), RepairStatus.PENDING_ARBITRATION.getCode()
    );

    private static final String SYSTEM_PROMPT =
    """
        你是校园报修重复判定助手。根据当前工单与候选历史工单，判断是否疑似重复报修。
        仅返回 JSON：{"suspected":true/false,"reason":"管理员可读的判定理由"}。
        不要返回历史工单编号或明细。
    """;

    @Autowired
    private RepairOrderMapper orders;

    @Autowired
    private AiClientGateway aiGateway;

    @Autowired
    private AiModelRouteService modelRoute;

    @Autowired
    private AiEmbeddingGateway embeddingGateway;

    // 检测草稿工单是否疑似重复，失败时不阻断提交流程
    public DuplicateRepairCheckVO detect(RepairOrder current, Long operatorId) {
        DuplicateRepairCheckVO vo = new DuplicateRepairCheckVO();
        vo.setSuspected(false);
        if (current == null || current.getCategoryId() == null) {
            return vo;
        }
        List<RepairOrder> candidates = findTopCandidates(current);
        if (candidates.isEmpty()) {
            return vo;
        }
        String userPrompt = buildPrompt(current, candidates);
        AiCallResult result = aiGateway.chat(AiSceneType.DUPLICATE_REPAIR, operatorId, current.getOrderId(), "REPAIR_ORDER", modelRoute.duplicateRepairModel(), SYSTEM_PROMPT, userPrompt);
        DuplicateJudgment judgment = parseJudgment(result);
        if (judgment != null && judgment.suspected()) {
            vo.setSuspected(true);
            vo.setReporterReminder("系统检测到您近期可能在相同位置或资产上报过类似故障，如确为新问题可继续提交。");
            vo.setDuplicateReason(judgment.reason());
        }
        return vo;
    }

    // 检索 Top3 候选历史工单
    public List<RepairOrder> findTopCandidates(RepairOrder current) {
        return findTopCandidatesInternal(current);
    }

    private List<RepairOrder> findTopCandidatesInternal(RepairOrder current) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<RepairOrder> pool = orders.selectList(Wrappers.<RepairOrder>lambdaQuery()
                .eq(RepairOrder::getDeleteState, 0).eq(RepairOrder::getCategoryId, current.getCategoryId())
                .in(RepairOrder::getStatus, IN_PROGRESS).ge(RepairOrder::getCreateTime, since)
                .ne(current.getOrderId() != null, RepairOrder::getOrderId, current.getOrderId())
                .and(w -> {
                    if (current.getAssetId() != null) {
                        w.eq(RepairOrder::getAssetId, current.getAssetId());
                    } else if (current.getCampusId() != null && current.getBuildingId() != null) {
                        w.eq(RepairOrder::getCampusId, current.getCampusId()).eq(RepairOrder::getBuildingId, current.getBuildingId());
                    } else if (current.getCampusId() != null) {
                        w.eq(RepairOrder::getCampusId, current.getCampusId());
                    } else {
                        w.apply("1=0");
                    }
                }));
        List<ScoredOrder> scored = new ArrayList<>();
        for (RepairOrder candidate : pool) {
            scored.add(new ScoredOrder(candidate, similarityScore(current, candidate)));
        }
        scored.sort(Comparator.comparingInt(ScoredOrder::score).reversed());
        List<RepairOrder> preRanked = scored.stream().map(ScoredOrder::order).limit(20).toList();
        String queryText = nullSafe(current.getTitle()) + " " + nullSafe(current.getDescription());
        List<RepairOrder> ranked = embeddingGateway.rankBySimilarity(AiSceneType.DUPLICATE_REPAIR, current.getReporterId(),
                queryText, preRanked, order -> nullSafe(order.getTitle()) + " " + nullSafe(order.getDescription()));
        return ranked.stream().limit(3).toList();
    }

    private int similarityScore(RepairOrder current, RepairOrder candidate) {
        int score = 0;
        if (current.getAssetId() != null && current.getAssetId().equals(candidate.getAssetId())) {
            score += 50;
        }
        if (current.getCampusId() != null && current.getCampusId().equals(candidate.getCampusId())) {
            score += 20;
        }
        if (current.getBuildingId() != null && current.getBuildingId().equals(candidate.getBuildingId())) {
            score += 20;
        }
        if (current.getTitle() != null && candidate.getTitle() != null
                && current.getTitle().equalsIgnoreCase(candidate.getTitle())) {
            score += 10;
        }
        if (current.getDescription() != null && candidate.getDescription() != null && candidate.getDescription().contains(current.getDescription().substring(0, Math.min(20, current.getDescription().length())))) {
            score += 10;
        }
        return score;
    }

    private String buildPrompt(RepairOrder current, List<RepairOrder> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("当前工单：标题=").append(nullSafe(current.getTitle())).append("，描述=").append(nullSafe(current.getDescription()))
                .append("，故障类型ID=").append(current.getCategoryId()).append("，资产ID=").append(current.getAssetId())
                .append("，校区ID=").append(current.getCampusId()).append("，楼栋ID=").append(current.getBuildingId()).append("\n");
        sb.append("候选历史工单（最多3条）：\n");
        int idx = 1;
        for (RepairOrder candidate : candidates) {
            sb.append(idx++).append(". 标题=").append(nullSafe(candidate.getTitle())).append("，描述=").append(nullSafe(candidate.getDescription()))
                    .append("，状态=").append(candidate.getStatus()).append("，资产ID=").append(candidate.getAssetId())
                    .append("，校区ID=").append(candidate.getCampusId()).append("，楼栋ID=").append(candidate.getBuildingId()).append("\n");
        }
        return sb.toString();
    }

    private DuplicateJudgment parseJudgment(AiCallResult result) {
        if (result == null || !result.isSuccess() || result.getContent() == null) {
            return null;
        }
        try {
            String content = result.getContent();
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return null;
            }
            String json = content.substring(start, end + 1);
            boolean suspected = json.contains("\"suspected\":true") || json.contains("\"suspected\": true");
            String reason = extractReason(json);
            return new DuplicateJudgment(suspected, reason);
        } catch (Exception ex) {
            log.warn("解析重复报修 AI 结果失败", ex);
            return null;
        }
    }

    private String extractReason(String json) {
        int idx = json.indexOf("\"reason\"");
        if (idx < 0) {
            return "AI 判定存在疑似重复报修";
        }
        int colon = json.indexOf(':', idx);
        int quoteStart = json.indexOf('"', colon + 1);
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteStart >= 0 && quoteEnd > quoteStart) {
            return json.substring(quoteStart + 1, quoteEnd);
        }
        return "AI 判定存在疑似重复报修";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private record DuplicateJudgment(boolean suspected, String reason) {}

    private record ScoredOrder(RepairOrder order, int score) {}
}
