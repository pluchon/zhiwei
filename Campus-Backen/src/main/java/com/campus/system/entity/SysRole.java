package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 系统固定角色实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_role")
public class SysRole extends BaseEntity {

    // 角色主键
    @TableId
    private Long roleId;

    // 角色编码名称
    private String roleName;

    // 角色状态（0正常 1停用）
    private Integer status;

    // 角色说明
    private String remark;
}
