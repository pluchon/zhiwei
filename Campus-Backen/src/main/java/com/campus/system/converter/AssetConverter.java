package com.campus.system.converter;

import com.campus.system.common.enums.AssetStatus;
import com.campus.system.entity.Asset;
import com.campus.system.entity.AssetCategory;
import com.campus.system.vo.AssetCategoryVO;
import com.campus.system.vo.AssetVO;
import java.util.List;
import java.util.function.Function;
import lombok.NoArgsConstructor;

// 资产相关实体到 VO 的转换
@NoArgsConstructor
public final class AssetConverter {

    public static AssetCategoryVO toCategoryVO(AssetCategory entity) {
        if (entity == null) {
            return null;
        }
        AssetCategoryVO vo = new AssetCategoryVO();
        vo.setAssetCategoryId(entity.getAssetCategoryId());
        vo.setCategoryName(entity.getCategoryName());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<AssetCategoryVO> toCategoryVOList(List<AssetCategory> entities) {
        return toList(entities, AssetConverter::toCategoryVO);
    }

    public static AssetVO toAssetVO(Asset entity) {
        if (entity == null) {
            return null;
        }
        AssetVO vo = new AssetVO();
        vo.setAssetId(entity.getAssetId());
        vo.setAssetNo(entity.getAssetNo());
        vo.setAssetName(entity.getAssetName());
        vo.setAssetCategoryId(entity.getAssetCategoryId());
        vo.setCampusId(entity.getCampusId());
        vo.setBuildingId(entity.getBuildingId());
        vo.setFloor(entity.getFloor());
        vo.setRoom(entity.getRoom());
        vo.setLocationDetail(entity.getLocationDetail());
        vo.setStatus(entity.getStatus());
        if (entity.getStatus() != null) {
            vo.setStatusLabel(AssetStatus.of(entity.getStatus()).getLabel());
        }
        vo.setDescription(entity.getDescription());
        vo.setEnabledDate(entity.getEnabledDate());
        vo.setPurchaseDate(entity.getPurchaseDate());
        vo.setImageObjectKey(entity.getImageObjectKey());
        vo.setActiveOrderId(entity.getActiveOrderId());
        vo.setHasActiveOrder(entity.getActiveOrderId() != null);
        vo.setVersion(entity.getVersion());
        vo.setDeleteState(entity.getDeleteState());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    public static List<AssetVO> toAssetVOList(List<Asset> entities) {
        return toList(entities, AssetConverter::toAssetVO);
    }

    private static <S, T> List<T> toList(List<S> entities, Function<S, T> mapper) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(mapper).toList();
    }
}
