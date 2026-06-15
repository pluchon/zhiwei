package com.campus.system.converter;

import com.campus.system.common.enums.SuggestionCategory;
import com.campus.system.common.enums.SuggestionStatus;
import com.campus.system.entity.RepairerSuggestion;
import com.campus.system.vo.RepairerSuggestionVO;
import java.util.List;
import java.util.function.Function;
import lombok.NoArgsConstructor;

// 维修师傅建议实体到 VO 的转换
@NoArgsConstructor
public final class RepairerSuggestionConverter {

    public static RepairerSuggestionVO toVO(RepairerSuggestion entity) {
        if (entity == null) {
            return null;
        }
        RepairerSuggestionVO vo = new RepairerSuggestionVO();
        vo.setSuggestionId(entity.getSuggestionId());
        vo.setRepairerId(entity.getRepairerId());
        vo.setCategory(entity.getCategory());
        if (entity.getCategory() != null) {
            vo.setCategoryLabel(SuggestionCategory.of(entity.getCategory()).getLabel());
        }
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        if (entity.getStatus() != null) {
            vo.setStatusLabel(SuggestionStatus.of(entity.getStatus()).getLabel());
        }
        vo.setWithdrawnFlag(entity.getWithdrawnFlag());
        vo.setAdminReply(entity.getAdminReply());
        vo.setHandlerId(entity.getHandlerId());
        vo.setHandledTime(entity.getHandledTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<RepairerSuggestionVO> toVOList(List<RepairerSuggestion> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(RepairerSuggestionConverter::toVO).toList();
    }
}
