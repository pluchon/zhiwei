package com.campus.system.dto;

import java.time.LocalDateTime;
import lombok.Data;

// 账号人工恢复申请查询参数
@Data
public class ManualRecoveryQueryDTO {

    // 目标用户主键
    private Long targetUserId;

    // 申请状态
    private String status;

    // 创建时间起
    private LocalDateTime createTimeFrom;

    // 创建时间止
    private LocalDateTime createTimeTo;

    // 仅待审批申请
    private Boolean onlyPending;
}
