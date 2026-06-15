package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.enums.AiSceneType;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.dto.RepairOrderLinkConfirmDTO;
import com.campus.system.entity.RepairOrder;
import com.campus.system.entity.RepairOrderAiLink;
import com.campus.system.mapper.RepairOrderAiLinkMapper;
import com.campus.system.mapper.RepairOrderMapper;
import com.campus.system.service.repair.DuplicateRepairDetectionService;
import com.campus.system.service.ai.AiCallResult;
import com.campus.system.service.ai.AiClientGateway;
import com.campus.system.service.ai.AiModelRouteService;
import com.campus.system.service.interfaces.RepairOrderAiService;
import com.campus.system.vo.RepairOrderAiAnalysisVO;
import com.campus.system.vo.RepairOrderAiLinkVO;
import com.campus.system.vo.RepairOrderDuplicateDetailVO;
import com.campus.system.dto.AiOrderLinkRecommendationDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 工单 AI 辅助业务实现
@Service
public class RepairOrderAiServiceImpl implements RepairOrderAiService {

    private static final Logger log = LoggerFactory.getLogger(RepairOrderAiServiceImpl.class);

    private static final String DISPATCH_SYSTEM =
            """
            你是校园报修派单辅助助手。根据工单信息输出文字分析，包括情况概述、处理建议和注意事项。
            不要推荐具体维修师傅姓名，不要输出排序名单，不要建议自动派单。仅输出纯文本分析。
            """;

    private static final String ORDER_LINK_SYSTEM =
            """
            你是校园报修工单关联推荐助手。根据当前工单与候选历史工单，判断哪些候选适合作为重复/关联工单。
            仅返回 JSON 数组，每项格式：{"orderId":数字,"recommended":true/false,"reason":"针对该候选的独立理由"}。
            可推荐部分候选，也可全部不推荐。orderId 必须来自候选列表。
            """;

    private static final String DEGRADE_REASON = "AI 关联推荐暂不可用，请结合候选工单人工判断。";

    @Autowired
    private RepairOrderMapper orders;

    @Autowired
    private RepairOrderAiLinkMapper links;

    @Autowired
    private DuplicateRepairDetectionService duplicateDetection;

    @Autowired
    private AiClientGateway aiGateway;

    @Autowired
    private AiModelRouteService modelRoute;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairOrderDuplicateDetailVO loadDuplicateDetail(Long orderId) {
        requireAdmin();
        RepairOrder order = requireOrder(orderId);
        RepairOrderDuplicateDetailVO vo = new RepairOrderDuplicateDetailVO();
        vo.setSuspectedDuplicate(order.getSuspectedDuplicate() != null && order.getSuspectedDuplicate() == 1);
        vo.setDuplicateReason(order.getDuplicateReason());
        if (!Boolean.TRUE.equals(vo.getSuspectedDuplicate())) {
            vo.setLinks(List.of());
            return vo;
        }
        ensureLinkRecommendations(order);
        vo.setLinks(loadLinks(orderId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmLink(RepairOrderLinkConfirmDTO body) {
        requireAdmin();
        if (body == null || body.getLinkId() == null) {
            throw BusinessException.badRequest("linkId 不能为空");
        }
        RepairOrderAiLink link = links.selectById(body.getLinkId());
        if (link == null || link.getDeleteState() != 0) {
            throw BusinessException.notFound("关联记录不存在");
        }
        if (links.confirmLink(body.getLinkId(), SecurityUtils.current().userId()) != 1) {
            throw BusinessException.conflict("关联状态已变化");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeLink(Long linkId) {
        requireAdmin();
        if (linkId == null) {
            throw BusinessException.badRequest("linkId 不能为空");
        }
        if (links.logicDeleteLink(linkId, SecurityUtils.current().userId()) != 1) {
            throw BusinessException.notFound("关联记录不存在");
        }
    }

    @Override
    public RepairOrderAiAnalysisVO analyzeDispatch(Long orderId) {
        requireAdmin();
        RepairOrder order = requireOrder(orderId);
        RepairOrderAiAnalysisVO vo = new RepairOrderAiAnalysisVO();
        String prompt = "工单标题：" + nullSafe(order.getTitle()) + "\n描述：" + nullSafe(order.getDescription())
                + "\n状态：" + order.getStatus() + "\n故障类型ID：" + order.getCategoryId()
                + "\n校区：" + nullSafe(order.getCampus()) + "\n楼栋：" + nullSafe(order.getBuilding());
        AiCallResult result = aiGateway.chat(AiSceneType.DISPATCH_ANALYSIS, SecurityUtils.current().userId(), orderId,
                "REPAIR_ORDER", modelRoute.chatModel(), DISPATCH_SYSTEM, prompt);
        if (result.isSuccess()) {
            vo.setAnalysisText(result.getContent());
            vo.setDegraded(result.isDegraded());
        } else {
            vo.setAnalysisText("AI 分析暂不可用，请根据工单信息人工判断。");
            vo.setDegraded(true);
        }
        return vo;
    }

    private void ensureLinkRecommendations(RepairOrder order) {
        RepairOrder fresh = orders.selectById(order.getOrderId());
        if (fresh != null && fresh.getAiLinkGenerated() != null && fresh.getAiLinkGenerated() == 1) {
            return;
        }
        long existing = links.selectCount(Wrappers.<RepairOrderAiLink>lambdaQuery()
                .eq(RepairOrderAiLink::getSourceOrderId, order.getOrderId())
                .eq(RepairOrderAiLink::getDeleteState, 0));
        if (existing > 0) {
            markLinkGenerated(order.getOrderId());
            return;
        }
        List<RepairOrder> candidates = duplicateDetection.findTopCandidates(order);
        if (candidates.isEmpty()) {
            markLinkGenerated(order.getOrderId());
            return;
        }
        Set<Long> allowedIds = new HashSet<>();
        for (RepairOrder candidate : candidates) {
            allowedIds.add(candidate.getOrderId());
        }
        String prompt = buildOrderLinkPrompt(order, candidates);
        AiCallResult result = aiGateway.chat(AiSceneType.ORDER_LINK, SecurityUtils.current().userId(), order.getOrderId(),
                "REPAIR_ORDER", modelRoute.chatModel(), ORDER_LINK_SYSTEM, prompt);
        List<AiOrderLinkRecommendationDTO> recommendations = parseLinkRecommendations(result, allowedIds);
        if (recommendations == null) {
            for (RepairOrder candidate : candidates) {
                insertLink(order.getOrderId(), candidate.getOrderId(), DEGRADE_REASON);
            }
        } else if (recommendations.isEmpty()) {
            // AI 判定全部不适合关联，不插入记录
        } else {
            for (AiOrderLinkRecommendationDTO recommendation : recommendations) {
                if (recommendation.isRecommended()) {
                    insertLink(order.getOrderId(), recommendation.getOrderId(), recommendation.getReason());
                }
            }
        }
        markLinkGenerated(order.getOrderId());
    }

    private void insertLink(Long sourceOrderId, Long targetOrderId, String reason) {
        long existing = links.selectCount(Wrappers.<RepairOrderAiLink>lambdaQuery()
                .eq(RepairOrderAiLink::getSourceOrderId, sourceOrderId)
                .eq(RepairOrderAiLink::getTargetOrderId, targetOrderId)
                .eq(RepairOrderAiLink::getDeleteState, 0));
        if (existing > 0) {
            return;
        }
        RepairOrderAiLink link = new RepairOrderAiLink();
        link.setSourceOrderId(sourceOrderId);
        link.setTargetOrderId(targetOrderId);
        link.setLinkType("DUPLICATE");
        link.setAiReason(reason != null && !reason.isBlank() ? reason : "AI 推荐关联历史工单");
        link.setConfirmed(0);
        links.insert(link);
    }

    private void markLinkGenerated(Long orderId) {
        orders.update(null, Wrappers.<RepairOrder>lambdaUpdate()
                .set(RepairOrder::getAiLinkGenerated, 1)
                .eq(RepairOrder::getOrderId, orderId));
    }

    private String buildOrderLinkPrompt(RepairOrder order, List<RepairOrder> candidates) {
        StringBuilder builder = new StringBuilder();
        builder.append("当前工单：标题=").append(nullSafe(order.getTitle()))
                .append("，描述=").append(nullSafe(order.getDescription()))
                .append("，故障类型ID=").append(order.getCategoryId()).append("\n");
        builder.append("候选历史工单：\n");
        for (RepairOrder candidate : candidates) {
            builder.append("- orderId=").append(candidate.getOrderId())
                    .append("，标题=").append(nullSafe(candidate.getTitle()))
                    .append("，描述=").append(nullSafe(candidate.getDescription()))
                    .append("，状态=").append(candidate.getStatus()).append("\n");
        }
        return builder.toString();
    }

    private List<AiOrderLinkRecommendationDTO> parseLinkRecommendations(AiCallResult result, Set<Long> allowedIds) {
        if (result == null || !result.isSuccess() || result.getContent() == null) {
            return null;
        }
        try {
            String content = result.getContent();
            int start = content.indexOf('[');
            int end = content.lastIndexOf(']');
            if (start < 0 || end <= start) {
                return null;
            }
            List<Map<String, Object>> rows = objectMapper.readValue(content.substring(start, end + 1), new TypeReference<>() {
            });
            List<AiOrderLinkRecommendationDTO> recommendations = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Long orderId = Long.valueOf(String.valueOf(row.get("orderId")));
                if (!allowedIds.contains(orderId)) {
                    continue;
                }
                AiOrderLinkRecommendationDTO item = new AiOrderLinkRecommendationDTO();
                item.setOrderId(orderId);
                item.setRecommended(Boolean.TRUE.equals(row.get("recommended"))
                        || "true".equalsIgnoreCase(String.valueOf(row.get("recommended"))));
                item.setReason(row.get("reason") == null ? "" : String.valueOf(row.get("reason")));
                recommendations.add(item);
            }
            return recommendations;
        } catch (Exception ex) {
            log.warn("解析 ORDER_LINK 结果失败", ex);
            return null;
        }
    }

    private List<RepairOrderAiLinkVO> loadLinks(Long orderId) {
        List<RepairOrderAiLink> records = links.selectList(Wrappers.<RepairOrderAiLink>lambdaQuery()
                .eq(RepairOrderAiLink::getSourceOrderId, orderId).eq(RepairOrderAiLink::getDeleteState, 0).orderByDesc(RepairOrderAiLink::getCreateTime));
        List<RepairOrderAiLinkVO> result = new ArrayList<>();
        for (RepairOrderAiLink record : records) {
            RepairOrderAiLinkVO vo = new RepairOrderAiLinkVO();
            vo.setLinkId(record.getLinkId());
            vo.setSourceOrderId(record.getSourceOrderId());
            vo.setTargetOrderId(record.getTargetOrderId());
            vo.setLinkType(record.getLinkType());
            vo.setAiReason(record.getAiReason());
            vo.setConfirmed(record.getConfirmed());
            RepairOrder target = orders.selectById(record.getTargetOrderId());
            if (target != null) {
                vo.setTargetOrderNo(target.getOrderNo());
                vo.setTargetOrderTitle(target.getTitle());
            }
            result.add(vo);
        }
        return result;
    }

    private RepairOrder requireOrder(Long orderId) {
        RepairOrder order = orders.selectById(orderId);
        if (order == null || order.getDeleteState() != 0) {
            throw BusinessException.notFound("工单不存在");
        }
        return order;
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可操作");
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
