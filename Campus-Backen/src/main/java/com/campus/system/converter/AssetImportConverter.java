package com.campus.system.converter;

import com.campus.system.common.enums.AssetImportItemStatus;
import com.campus.system.common.enums.AssetImportSourceType;
import com.campus.system.common.enums.AiRecognizeStatus;
import com.campus.system.entity.AssetImportBatch;
import com.campus.system.entity.AssetImportItem;
import com.campus.system.vo.AssetImportBatchVO;
import com.campus.system.vo.AssetImportItemVO;
import java.util.List;
import java.util.function.Function;
import lombok.NoArgsConstructor;

// 资产导入相关实体到 VO 的转换
@NoArgsConstructor
public final class AssetImportConverter {

    public static AssetImportBatchVO toBatchVO(AssetImportBatch entity) {
        if (entity == null) {
            return null;
        }
        AssetImportBatchVO vo = new AssetImportBatchVO();
        vo.setBatchId(entity.getBatchId());
        vo.setFileName(entity.getFileName());
        vo.setOperatorId(entity.getOperatorId());
        vo.setTotalCount(entity.getTotalCount());
        vo.setPendingCount(entity.getPendingCount());
        vo.setConfirmedCount(entity.getConfirmedCount());
        vo.setIgnoredCount(entity.getIgnoredCount());
        vo.setSourceType(entity.getSourceType());
        if (entity.getSourceType() != null) {
            vo.setSourceTypeLabel(AssetImportSourceType.of(entity.getSourceType()).getLabel());
        }
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    public static List<AssetImportBatchVO> toBatchVOList(List<AssetImportBatch> entities) {
        return toList(entities, AssetImportConverter::toBatchVO);
    }

    public static AssetImportItemVO toItemVO(AssetImportItem entity) {
        if (entity == null) {
            return null;
        }
        AssetImportItemVO vo = new AssetImportItemVO();
        vo.setItemId(entity.getItemId());
        vo.setBatchId(entity.getBatchId());
        vo.setRowNumber(entity.getRowNumber());
        vo.setAssetName(entity.getAssetName());
        vo.setCategoryText(entity.getCategoryText());
        vo.setAssetCategoryId(entity.getAssetCategoryId());
        vo.setPurchaseDate(entity.getPurchaseDate());
        vo.setEnabledDate(entity.getEnabledDate());
        vo.setAssetDescription(entity.getAssetDescription());
        vo.setSourceImageObjectKey(entity.getSourceImageObjectKey());
        vo.setAiRecognizeStatus(entity.getAiRecognizeStatus());
        if (entity.getAiRecognizeStatus() != null) {
            vo.setAiRecognizeStatusLabel(AiRecognizeStatus.of(entity.getAiRecognizeStatus()).getLabel());
        }
        vo.setLocationText(entity.getLocationText());
        vo.setCampusId(entity.getCampusId());
        vo.setBuildingId(entity.getBuildingId());
        vo.setFloor(entity.getFloor());
        vo.setRoom(entity.getRoom());
        vo.setLocationDetail(entity.getLocationDetail());
        vo.setStatus(entity.getStatus());
        if (entity.getStatus() != null) {
            vo.setStatusLabel(AssetImportItemStatus.of(entity.getStatus()).getLabel());
        }
        vo.setDuplicateHint(entity.getDuplicateHint());
        vo.setFailureReason(entity.getFailureReason());
        vo.setConfirmedAssetId(entity.getConfirmedAssetId());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    public static List<AssetImportItemVO> toItemVOList(List<AssetImportItem> entities) {
        return toList(entities, AssetImportConverter::toItemVO);
    }

    private static <S, T> List<T> toList(List<S> entities, Function<S, T> mapper) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(mapper).toList();
    }
}
