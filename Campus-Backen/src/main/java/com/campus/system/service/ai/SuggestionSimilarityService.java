package com.campus.system.service.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.enums.AiSceneType;
import com.campus.system.common.enums.SuggestionStatus;
import com.campus.system.dto.RepairerSuggestionSubmitDTO;
import com.campus.system.entity.RepairerSuggestion;
import com.campus.system.mapper.RepairerSuggestionMapper;
import com.campus.system.vo.SuggestionSimilarityVO;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 维修师傅建议相似检测
@Component
public class SuggestionSimilarityService {

    @Autowired
    private AiEmbeddingGateway embeddingGateway;

    @Autowired
    private RepairerSuggestionMapper suggestionMapper;

    // 提交前检测相似建议，失败时不阻止后续提交
    public SuggestionSimilarityVO detect(Long repairerId, RepairerSuggestionSubmitDTO body, Long excludeSuggestionId) {
        SuggestionSimilarityVO vo = new SuggestionSimilarityVO();
        vo.setHasSimilar(false);
        if (body == null || body.getTitle() == null || body.getContent() == null) {
            return vo;
        }
        String probe = body.getTitle().trim() + " " + body.getContent().trim();
        List<RepairerSuggestion> pool = suggestionMapper.selectList(Wrappers.<RepairerSuggestion>lambdaQuery()
                .eq(RepairerSuggestion::getDeleteState, 0).eq(RepairerSuggestion::getWithdrawnFlag, 0)
                .ne(excludeSuggestionId != null, RepairerSuggestion::getSuggestionId, excludeSuggestionId)
                .orderByDesc(RepairerSuggestion::getCreateTime).last("limit 200"));
        if (pool.isEmpty()) {
            return vo;
        }
        RepairerSuggestion ownMatch = findOwnSimilar(repairerId, probe, pool);
        if (ownMatch != null) {
            vo.setHasSimilar(true);
            vo.setOthersSimilar(false);
            vo.setSuggestionId(ownMatch.getSuggestionId());
            vo.setTitle(ownMatch.getTitle());
            vo.setStatus(ownMatch.getStatus());
            vo.setStatusLabel(statusLabel(ownMatch.getStatus()));
            vo.setMessage("检测到您曾提交过内容相似的建议，如确为新建议可继续提交。");
            return vo;
        }
        boolean others = findOthersSimilar(repairerId, probe, pool);
        if (others) {
            vo.setHasSimilar(true);
            vo.setOthersSimilar(true);
            vo.setMessage("系统中存在内容相似的建议，如确为新建议可继续提交。");
        }
        return vo;
    }

    private RepairerSuggestion findOwnSimilar(Long repairerId, String probe, List<RepairerSuggestion> pool) {
        for (RepairerSuggestion suggestion : pool) {
            if (!repairerId.equals(suggestion.getRepairerId())) {
                continue;
            }
            if (isSimilar(probe, suggestion, repairerId)) {
                return suggestion;
            }
        }
        return null;
    }

    private boolean findOthersSimilar(Long repairerId, String probe, List<RepairerSuggestion> pool) {
        for (RepairerSuggestion suggestion : pool) {
            if (repairerId.equals(suggestion.getRepairerId())) {
                continue;
            }
            if (isSimilar(probe, suggestion, repairerId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSimilar(String probe, RepairerSuggestion suggestion, Long operatorId) {
        String target = nullSafe(suggestion.getTitle()) + " " + nullSafe(suggestion.getContent());
        if (embeddingGateway.isAvailable()) {
            return embeddingGateway.isSimilar(AiSceneType.SUGGESTION_SIMILARITY, operatorId, probe, target);
        }
        return keywordSimilar(probe, target);
    }

    private boolean keywordSimilar(String left, String right) {
        String normalizedLeft = left.replaceAll("\\s+", "").toLowerCase();
        String normalizedRight = right.replaceAll("\\s+", "").toLowerCase();
        if (normalizedLeft.length() < 4 || normalizedRight.length() < 4) {
            return false;
        }
        return normalizedLeft.contains(normalizedRight.substring(0, Math.min(12, normalizedRight.length())))
                || normalizedRight.contains(normalizedLeft.substring(0, Math.min(12, normalizedLeft.length())));
    }

    private String statusLabel(String status) {
        try {
            return SuggestionStatus.of(status).getLabel();
        } catch (Exception ex) {
            return status;
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
