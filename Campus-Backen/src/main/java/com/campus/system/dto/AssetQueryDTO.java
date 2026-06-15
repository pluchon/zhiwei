package com.campus.system.dto;

import lombok.Data;

// 资产台账查询请求参数
@Data
public class AssetQueryDTO {

    // 资产编号
    private String assetNo;

    // 资产名称关键词
    private String assetNameKeyword;

    // 资产分类主键
    private Long assetCategoryId;

    // 校区主键
    private Long campusId;

    // 楼栋主键
    private Long buildingId;

    // 资产状态
    private String status;

    // 是否包含已逻辑删除资产，仅管理员恢复场景使用
    private Boolean includeDeleted;
}
