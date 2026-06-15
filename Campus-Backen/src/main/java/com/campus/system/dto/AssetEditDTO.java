package com.campus.system.dto;

import java.time.LocalDate;
import lombok.Data;

// 资产台账新增与编辑请求参数
@Data
public class AssetEditDTO {

    // 资产名称
    private String assetName;

    // 资产分类主键
    private Long assetCategoryId;

    // 所属校区主键
    private Long campusId;

    // 所属楼栋主键
    private Long buildingId;

    // 所在楼层
    private String floor;

    // 教室或房间
    private String room;

    // 具体位置描述
    private String locationDetail;

    // 资产说明
    private String description;

    // 启用日期
    private LocalDate enabledDate;

    // 购入日期
    private LocalDate purchaseDate;

    // 资产图片 OSS 对象键
    private String imageObjectKey;

    // 编辑时携带的乐观锁版本号
    private Integer version;
}
