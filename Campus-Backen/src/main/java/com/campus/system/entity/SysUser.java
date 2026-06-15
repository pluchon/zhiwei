package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 系统用户实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_user")
public class SysUser extends BaseEntity {

    // 用户主键
    @TableId
    private Long userId;

    // 登录账号
    private String userNo;

    // 用户真实姓名
    private String realName;

    // 用户昵称
    private String nickName;

    // 角色主键
    private Long roleId;

    // 已绑定邮箱
    private String email;

    // 已绑定手机号码
    private String phoneNumber;

    // 家长手机号码
    private String parentPhone;

    // 用户头像地址
    private String avatar;

    // BCrypt 密码摘要
    private String password;

    // 激活状态（0未激活 1已激活）
    private Integer activationStatus;

    // 账号状态（0正常 1停用）
    private Integer accountStatus;

    // 安全戳（会话失效标记）
    private String securityStamp;

    // 手机确认要求（0无需确认 1登录后需要重新确认）
    private Integer phoneConfirmRequired;

    // 接单可用状态
    private String acceptingState;

    // 暂停接单原因
    private String pauseReason;

    // 预计恢复接单时间
    private LocalDateTime expectedResumeTime;
}
