package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 系统字典类型实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    // 字典类型主键
    @TableId
    private Long dictTypeId;

    // 字典显示名称
    private String dictName;

    // 字典类型编码
    private String dictType;

    // 字典类型状态（0正常 1停用）
    private Integer status;

    // 字典类型备注
    private String remark;
}
