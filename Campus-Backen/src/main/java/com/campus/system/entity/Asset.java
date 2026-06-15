package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 资产台账实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("asset")
public class Asset extends BaseEntity {

    // 资产主键
    @TableId
    private Long assetId;

    // 资产编号
    private String assetNo;

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

    // 资产状态
    private String status;

    // 资产说明
    private String description;

    // 启用日期
    private LocalDate enabledDate;

    // 购入日期
    private LocalDate purchaseDate;

    // 资产图片 OSS 对象键
    private String imageObjectKey;

    // 当前未结束关联工单主键
    private Long activeOrderId;

    // 乐观锁版本号
    private Integer version;
}
