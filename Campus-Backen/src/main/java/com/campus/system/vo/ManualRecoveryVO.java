package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 账号人工恢复申请响应数据
@Data
public class ManualRecoveryVO {

    // 恢复申请主键
    private Long recoveryId;

    // 目标用户主键
    private Long targetUserId;

    // 目标用户账号
    private String targetUserNo;

    // 目标用户姓名
    private String targetRealName;

    // 脱敏后的新主手机号
    private String maskedNewPhone;

    // 申请状态
    private String status;

    // 申请状态中文名称
    private String statusLabel;

    // 发起管理员主键
    private Long applicantAdminId;

    // 发起管理员姓名
    private String applicantAdminName;

    // 复核管理员主键
    private Long reviewerAdminId;

    // 复核管理员姓名
    private String reviewerAdminName;

    // 线下身份核验说明
    private String identityCheckNote;

    // 审批处理说明
    private String reviewNote;

    // 审批通过时间
    private LocalDateTime approvedTime;

    // 新手机号验证截止时间
    private LocalDateTime expireTime;

    // 完成换绑时间
    private LocalDateTime completedTime;

    // 创建时间
    private LocalDateTime createTime;
}
