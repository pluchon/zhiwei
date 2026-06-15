package com.campus.system.converter;

import com.campus.system.common.enums.ManualRecoveryStatus;
import com.campus.system.entity.ManualAccountRecovery;
import com.campus.system.vo.ManualRecoveryVO;
import java.util.List;
import java.util.function.Function;
import lombok.NoArgsConstructor;

// 账号人工恢复申请实体到 VO 的转换
@NoArgsConstructor
public final class ManualAccountRecoveryConverter {

    public static ManualRecoveryVO toVO(ManualAccountRecovery entity) {
        if (entity == null) {
            return null;
        }
        ManualRecoveryVO vo = new ManualRecoveryVO();
        vo.setRecoveryId(entity.getRecoveryId());
        vo.setTargetUserId(entity.getTargetUserId());
        vo.setStatus(entity.getStatus());
        if (entity.getStatus() != null) {
            vo.setStatusLabel(ManualRecoveryStatus.of(entity.getStatus()).getLabel());
        }
        vo.setApplicantAdminId(entity.getApplicantAdminId());
        vo.setReviewerAdminId(entity.getReviewerAdminId());
        vo.setIdentityCheckNote(entity.getIdentityCheckNote());
        vo.setReviewNote(entity.getReviewNote());
        vo.setApprovedTime(entity.getApprovedTime());
        vo.setExpireTime(entity.getExpireTime());
        vo.setCompletedTime(entity.getCompletedTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    public static List<ManualRecoveryVO> toVOList(List<ManualAccountRecovery> entities) {
        return toList(entities, ManualAccountRecoveryConverter::toVO);
    }

    private static <S, T> List<T> toList(List<S> entities, Function<S, T> mapper) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(mapper).toList();
    }
}
