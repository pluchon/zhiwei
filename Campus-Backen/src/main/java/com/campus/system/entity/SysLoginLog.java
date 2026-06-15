package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 登录审计日志实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_login_log")
public class SysLoginLog extends BaseEntity {

    // 登录日志主键
    @TableId
    private Long loginLogId;

    // 登录用户主键
    private Long userId;

    // 登录标识（账号、手机号等）
    private String loginIdentifier;

    // 登录方式
    private String loginType;

    // 登录结果状态（0成功 1失败）
    private Integer status;

    // 登录结果说明
    private String message;

    // 登录来源 IP
    private String loginIp;
}
