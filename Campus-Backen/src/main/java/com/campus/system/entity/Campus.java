package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 校区基础数据实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("campus")
public class Campus extends BaseEntity {

    // 校区主键
    @TableId
    private Long campusId;

    // 校区展示名称
    private String campusName;

    // 唯一比较值
    private String normalizedName;

    // 拼音首字母排序键
    private String nameSortKey;

    // 校区说明
    private String description;

    // 启用状态（0启用 1停用）
    private Integer status;
}
