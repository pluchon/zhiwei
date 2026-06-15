package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 用户公开响应数据
@Data
public class UserVO {

    // 用户主键
    private Long userId;

    // 登录账号
    private String userNo;

    // 真实姓名
    private String realName;

    // 页面展示昵称
    private String nickName;

    // 当前角色主键
    private Long roleId;

    // 已绑定邮箱
    private String email;

    // 已绑定手机号
    private String phoneNumber;

    // 家长手机号，可为空
    private String parentPhone;

    // 用户头像地址
    private String avatar;

    // 激活状态：0 未激活，1 已激活
    private Integer activationStatus;

    // 账号状态：0 正常，1 停用
    private Integer accountStatus;

    // 是否要求重新确认手机号：0 否，1 是
    private Integer phoneConfirmRequired;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
