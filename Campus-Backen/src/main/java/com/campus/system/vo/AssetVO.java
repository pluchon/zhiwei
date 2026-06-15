package com.campus.system.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

// 资产台账响应数据
@Data
public class AssetVO {

    // 资产主键
    private Long assetId;

    // 资产编号
    private String assetNo;

    // 资产名称
    private String assetName;

    // 资产分类主键
    private Long assetCategoryId;

    // 资产分类名称
    private String assetCategoryName;

    // 校区主键
    private Long campusId;

    // 校区名称
    private String campusName;

    // 楼栋主键
    private Long buildingId;

    // 楼栋名称
    private String buildingName;

    // 所在楼层
    private String floor;

    // 教室或房间
    private String room;

    // 具体位置描述
    private String locationDetail;

    // 资产状态
    private String status;

    // 资产状态中文名称
    private String statusLabel;

    // 资产说明
    private String description;

    // 启用日期
    private LocalDate enabledDate;

    // 购入日期
    private LocalDate purchaseDate;

    // 已购入年数
    private Integer purchaseYears;

    // 已购入月数（扣除整年后剩余月数）
    private Integer purchaseMonths;

    // 资产图片 OSS 对象键
    private String imageObjectKey;

    // 资产图片访问地址
    private String imageSignedUrl;

    // 当前未结束关联工单主键
    private Long activeOrderId;

    // 是否存在未结束关联工单
    private Boolean hasActiveOrder;

    // 乐观锁版本号
    private Integer version;

    // 逻辑删除状态
    private Integer deleteState;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
