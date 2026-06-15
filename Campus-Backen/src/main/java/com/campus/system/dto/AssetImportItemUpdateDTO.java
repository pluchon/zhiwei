package com.campus.system.dto;

import java.time.LocalDate;
import lombok.Data;

// 待审核资产卡片编辑请求参数
@Data
public class AssetImportItemUpdateDTO {

    // 资产名称
    private String assetName;

    // 资产分类主键
    private Long assetCategoryId;

    // 购入日期
    private LocalDate purchaseDate;

    // 启用日期
    private LocalDate enabledDate;

    // 资产说明
    private String assetDescription;

    // 识别位置文本
    private String locationText;

    // 校区主键
    private Long campusId;

    // 楼栋主键
    private Long buildingId;

    // 所在楼层
    private String floor;

    // 教室或房间
    private String room;

    // 具体位置描述
    private String locationDetail;
}
