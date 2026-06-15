package com.campus.system.converter;

import com.campus.system.entity.Building;
import com.campus.system.entity.Campus;
import com.campus.system.entity.RepairAttachment;
import com.campus.system.entity.RepairCapability;
import com.campus.system.entity.RepairCategory;
import com.campus.system.entity.RepairComment;
import com.campus.system.entity.RepairEvaluation;
import com.campus.system.entity.RepairOrder;
import com.campus.system.entity.RepairOrderLog;
import com.campus.system.entity.RepairRecord;
import com.campus.system.entity.SysDictData;
import com.campus.system.entity.SysDictType;
import com.campus.system.entity.SysLoginLog;
import com.campus.system.entity.SysOperationLog;
import com.campus.system.entity.SysUser;
import com.campus.system.entity.UserNotification;
import com.campus.system.vo.BuildingVO;
import com.campus.system.vo.CampusVO;
import com.campus.system.vo.RepairAttachmentVO;
import com.campus.system.vo.RepairCapabilityVO;
import com.campus.system.vo.RepairCategoryVO;
import com.campus.system.vo.RepairCommentVO;
import com.campus.system.vo.RepairEvaluationVO;
import com.campus.system.vo.RepairOrderLogVO;
import com.campus.system.vo.RepairOrderVO;
import com.campus.system.vo.RepairRecordVO;
import com.campus.system.vo.SysDictDataVO;
import com.campus.system.vo.SysDictTypeVO;
import com.campus.system.vo.SysLoginLogVO;
import com.campus.system.vo.SysOperationLogVO;
import com.campus.system.vo.UserNotificationVO;
import com.campus.system.vo.UserVO;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;

// 数据库实体到前端 VO 的统一转换工具。
@NoArgsConstructor
public final class EntityVOConverter {

    public static UserVO toUserVO(SysUser entity) {
        if (entity == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setUserId(entity.getUserId());
        vo.setUserNo(entity.getUserNo());
        vo.setRealName(entity.getRealName());
        vo.setNickName(entity.getNickName());
        vo.setRoleId(entity.getRoleId());
        vo.setEmail(entity.getEmail());
        vo.setPhoneNumber(entity.getPhoneNumber());
        vo.setParentPhone(entity.getParentPhone());
        vo.setAvatar(entity.getAvatar());
        vo.setActivationStatus(entity.getActivationStatus());
        vo.setAccountStatus(entity.getAccountStatus());
        vo.setPhoneConfirmRequired(entity.getPhoneConfirmRequired());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<UserVO> toUserVOList(List<SysUser> entities) {
        return toList(entities, EntityVOConverter::toUserVO);
    }

    public static RepairCategoryVO toRepairCategoryVO(RepairCategory entity) {
        if (entity == null) {
            return null;
        }
        RepairCategoryVO vo = new RepairCategoryVO();
        vo.setCategoryId(entity.getCategoryId());
        vo.setCategoryName(entity.getCategoryName());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<RepairCategoryVO> toRepairCategoryVOList(List<RepairCategory> entities) {
        return toList(entities, EntityVOConverter::toRepairCategoryVO);
    }

    public static RepairCapabilityVO toRepairCapabilityVO(RepairCapability entity) {
        if (entity == null) {
            return null;
        }
        RepairCapabilityVO vo = new RepairCapabilityVO();
        vo.setCapabilityId(entity.getCapabilityId());
        vo.setRepairerId(entity.getRepairerId());
        vo.setCategoryId(entity.getCategoryId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<RepairCapabilityVO> toRepairCapabilityVOList(List<RepairCapability> entities) {
        return toList(entities, EntityVOConverter::toRepairCapabilityVO);
    }

    public static SysDictTypeVO toSysDictTypeVO(SysDictType entity) {
        if (entity == null) {
            return null;
        }
        SysDictTypeVO vo = new SysDictTypeVO();
        vo.setDictTypeId(entity.getDictTypeId());
        vo.setDictName(entity.getDictName());
        vo.setDictType(entity.getDictType());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<SysDictTypeVO> toSysDictTypeVOList(List<SysDictType> entities) {
        return toList(entities, EntityVOConverter::toSysDictTypeVO);
    }

    public static SysDictDataVO toSysDictDataVO(SysDictData entity) {
        if (entity == null) {
            return null;
        }
        SysDictDataVO vo = new SysDictDataVO();
        vo.setDictDataId(entity.getDictDataId());
        vo.setDictType(entity.getDictType());
        vo.setDictLabel(entity.getDictLabel());
        vo.setDictValue(entity.getDictValue());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<SysDictDataVO> toSysDictDataVOList(List<SysDictData> entities) {
        return toList(entities, EntityVOConverter::toSysDictDataVO);
    }

    public static SysLoginLogVO toSysLoginLogVO(SysLoginLog entity) {
        if (entity == null) {
            return null;
        }
        SysLoginLogVO vo = new SysLoginLogVO();
        vo.setLoginLogId(entity.getLoginLogId());
        vo.setUserId(entity.getUserId());
        vo.setLoginIdentifier(entity.getLoginIdentifier());
        vo.setLoginType(entity.getLoginType());
        vo.setStatus(entity.getStatus());
        vo.setMessage(entity.getMessage());
        vo.setLoginIp(entity.getLoginIp());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<SysLoginLogVO> toSysLoginLogVOList(List<SysLoginLog> entities) {
        return toList(entities, EntityVOConverter::toSysLoginLogVO);
    }

    public static SysOperationLogVO toSysOperationLogVO(SysOperationLog entity) {
        if (entity == null) {
            return null;
        }
        SysOperationLogVO vo = new SysOperationLogVO();
        vo.setOperationLogId(entity.getOperationLogId());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperationType(entity.getOperationType());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        vo.setDescription(entity.getDescription());
        vo.setOperationIp(entity.getOperationIp());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<SysOperationLogVO> toSysOperationLogVOList(List<SysOperationLog> entities) {
        return toList(entities, EntityVOConverter::toSysOperationLogVO);
    }

    public static CampusVO toCampusVO(Campus entity) {
        if (entity == null) {
            return null;
        }
        CampusVO vo = new CampusVO();
        vo.setCampusId(entity.getCampusId());
        vo.setCampusName(entity.getCampusName());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setDeleteState(entity.getDeleteState());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<CampusVO> toCampusVOList(List<Campus> entities) {
        return toList(entities, EntityVOConverter::toCampusVO);
    }

    public static BuildingVO toBuildingVO(Building entity) {
        if (entity == null) {
            return null;
        }
        BuildingVO vo = new BuildingVO();
        vo.setBuildingId(entity.getBuildingId());
        vo.setCampusId(entity.getCampusId());
        vo.setBuildingName(entity.getBuildingName());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setDeleteState(entity.getDeleteState());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<BuildingVO> toBuildingVOList(List<Building> entities) {
        return toList(entities, EntityVOConverter::toBuildingVO);
    }

    public static RepairOrderVO toRepairOrderVO(RepairOrder entity) {
        if (entity == null) {
            return null;
        }
        RepairOrderVO vo = new RepairOrderVO();
        vo.setOrderId(entity.getOrderId());
        vo.setOrderNo(entity.getOrderNo());
        vo.setRequestId(entity.getRequestId());
        vo.setReporterId(entity.getReporterId());
        vo.setReporterRoleId(entity.getReporterRoleId());
        vo.setReporterNickname(entity.getReporterNickname());
        vo.setReporterAvatar(entity.getReporterAvatar());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setCategoryId(entity.getCategoryId());
        vo.setRepairType(entity.getRepairType());
        vo.setAssetId(entity.getAssetId());
        vo.setAssetNoSnapshot(entity.getAssetNoSnapshot());
        vo.setAssetNameSnapshot(entity.getAssetNameSnapshot());
        vo.setAssetCategorySnapshot(entity.getAssetCategorySnapshot());
        vo.setAssetLocationSnapshot(entity.getAssetLocationSnapshot());
        vo.setCampusId(entity.getCampusId());
        vo.setCampus(entity.getCampus());
        vo.setCampusDescriptionSnapshot(entity.getCampusDescriptionSnapshot());
        vo.setBuildingId(entity.getBuildingId());
        vo.setBuilding(entity.getBuilding());
        vo.setBuildingDescriptionSnapshot(entity.getBuildingDescriptionSnapshot());
        vo.setFloor(entity.getFloor());
        vo.setRoom(entity.getRoom());
        vo.setLocationDetail(entity.getLocationDetail());
        vo.setContactPhone(entity.getContactPhone());
        vo.setStatus(entity.getStatus());
        vo.setCurrentRepairerId(entity.getCurrentRepairerId());
        vo.setUnresolvedCount(entity.getUnresolvedCount());
        vo.setVersion(entity.getVersion());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setCompletionTime(entity.getCompletionTime());
        vo.setAutoCompletedTime(entity.getAutoCompletedTime());
        vo.setExportedFlag(entity.getExportedFlag());
        vo.setFirstExportTime(entity.getFirstExportTime());
        vo.setSuspectedDuplicate(entity.getSuspectedDuplicate());
        vo.setDuplicateReason(entity.getDuplicateReason());
        return vo;
    }

    public static List<RepairOrderVO> toRepairOrderVOList(List<RepairOrder> entities) {
        return toList(entities, EntityVOConverter::toRepairOrderVO);
    }

    public static RepairAttachmentVO toRepairAttachmentVO(RepairAttachment entity) {
        if (entity == null) {
            return null;
        }
        RepairAttachmentVO vo = new RepairAttachmentVO();
        vo.setAttachmentId(entity.getAttachmentId());
        vo.setOrderId(entity.getOrderId());
        vo.setRecordId(entity.getRecordId());
        vo.setObjectKey(entity.getObjectKey());
        vo.setUploaderId(entity.getUploaderId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<RepairAttachmentVO> toRepairAttachmentVOList(List<RepairAttachment> entities) {
        return toList(entities, EntityVOConverter::toRepairAttachmentVO);
    }

    public static RepairRecordVO toRepairRecordVO(RepairRecord entity) {
        if (entity == null) {
            return null;
        }
        RepairRecordVO vo = new RepairRecordVO();
        vo.setRecordId(entity.getRecordId());
        vo.setOrderId(entity.getOrderId());
        vo.setRepairerId(entity.getRepairerId());
        vo.setResultDescription(entity.getResultDescription());
        vo.setAttemptNo(entity.getAttemptNo());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<RepairRecordVO> toRepairRecordVOList(List<RepairRecord> entities) {
        return toList(entities, EntityVOConverter::toRepairRecordVO);
    }

    public static RepairCommentVO toRepairCommentVO(RepairComment entity) {
        if (entity == null) {
            return null;
        }
        RepairCommentVO vo = new RepairCommentVO();
        vo.setCommentId(entity.getCommentId());
        vo.setOrderId(entity.getOrderId());
        vo.setAuthorId(entity.getAuthorId());
        vo.setCommentType(entity.getCommentType());
        vo.setContent(entity.getContent());
        vo.setIsPinned(entity.getIsPinned());
        vo.setIsWithdrawn(entity.getIsWithdrawn());
        vo.setWithdrawTime(entity.getWithdrawTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<RepairCommentVO> toRepairCommentVOList(List<RepairComment> entities) {
        return toList(entities, EntityVOConverter::toRepairCommentVO);
    }

    public static RepairOrderLogVO toRepairOrderLogVO(RepairOrderLog entity) {
        if (entity == null) {
            return null;
        }
        RepairOrderLogVO vo = new RepairOrderLogVO();
        vo.setLogId(entity.getLogId());
        vo.setOrderId(entity.getOrderId());
        vo.setOperatorId(entity.getOperatorId());
        vo.setAction(entity.getAction());
        vo.setFromStatus(entity.getFromStatus());
        vo.setToStatus(entity.getToStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<RepairOrderLogVO> toRepairOrderLogVOList(List<RepairOrderLog> entities) {
        return toList(entities, EntityVOConverter::toRepairOrderLogVO);
    }

    public static RepairEvaluationVO toRepairEvaluationVO(RepairEvaluation entity) {
        if (entity == null) {
            return null;
        }
        RepairEvaluationVO vo = new RepairEvaluationVO();
        vo.setEvaluationId(entity.getEvaluationId());
        vo.setOrderId(entity.getOrderId());
        vo.setRepairerId(entity.getRepairerId());
        vo.setStar(entity.getStar());
        vo.setContent(entity.getContent());
        vo.setFollowUpContent(entity.getFollowUpContent());
        vo.setFollowUpTime(entity.getFollowUpTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<RepairEvaluationVO> toRepairEvaluationVOList(List<RepairEvaluation> entities) {
        return toList(entities, EntityVOConverter::toRepairEvaluationVO);
    }

    public static UserNotificationVO toUserNotificationVO(UserNotification entity) {
        if (entity == null) {
            return null;
        }
        UserNotificationVO vo = new UserNotificationVO();
        vo.setNotificationId(entity.getNotificationId());
        vo.setReceiverId(entity.getReceiverId());
        vo.setOrderId(entity.getOrderId());
        vo.setSuggestionId(entity.getSuggestionId());
        vo.setNotificationType(entity.getNotificationType());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setIsRead(entity.getIsRead());
        vo.setReadTime(entity.getReadTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<UserNotificationVO> toUserNotificationVOList(List<UserNotification> entities) {
        return toList(entities, EntityVOConverter::toUserNotificationVO);
    }

    // 列表转换统一做空值保护，避免 ServiceImpl 重复判断空集合
    private static <E, V> List<V> toList(List<E> entities, Function<E, V> mapper) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(mapper).toList();
    }
}
