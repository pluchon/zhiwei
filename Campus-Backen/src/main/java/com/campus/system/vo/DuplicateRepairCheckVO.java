package com.campus.system.vo;

import lombok.Data;

// 重复报修检测响应
@Data
public class DuplicateRepairCheckVO {

    // 是否疑似重复
    private Boolean suspected;

    // 报修人可见的概括提醒
    private String reporterReminder;

    // 管理员可见的判定理由
    private String duplicateReason;
}
