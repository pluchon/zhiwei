package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 账号人工恢复申请实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("manual_account_recovery")
public class ManualAccountRecovery extends BaseEntity {

    // 恢复申请主键
    @TableId
    private Long recoveryId;

    // 目标用户主键
    private Long targetUserId;

    // 待绑定新主手机号
    private String newPhone;

    // 申请状态
    private String status;

    // 发起管理员主键
    private Long applicantAdminId;

    // 复核管理员主键
    private Long reviewerAdminId;

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
}
