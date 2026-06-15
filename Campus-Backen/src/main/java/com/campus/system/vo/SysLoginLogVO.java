package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 登录日志响应数据
@Data
public class SysLoginLogVO {

    // 登录日志主键
    private Long loginLogId;

    // 登录用户主键，可为空
    private Long userId;

    // 登录标识，如账号、手机号或邮箱
    private String loginIdentifier;

    // 登录方式
    private String loginType;

    // 登录结果：0 成功，1 失败
    private Integer status;

    // 登录结果说明
    private String message;

    // 登录来源 IP
    private String loginIp;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
