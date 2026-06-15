package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 楼栋基础数据实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("building")
public class Building extends BaseEntity {

    // 楼栋主键
    @TableId
    private Long buildingId;

    // 所属校区主键
    private Long campusId;

    // 楼栋展示名称
    private String buildingName;

    // 唯一比较值
    private String normalizedName;

    // 拼音首字母排序键
    private String nameSortKey;

    // 楼栋说明
    private String description;

    // 启用状态（0启用 1停用）
    private Integer status;
}
