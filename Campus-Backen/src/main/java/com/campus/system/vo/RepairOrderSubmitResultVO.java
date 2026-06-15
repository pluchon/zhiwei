package com.campus.system.vo;

import lombok.Data;

// 工单提交响应（含重复报修提醒）
@Data
public class RepairOrderSubmitResultVO {

    // 提交后的工单信息
    private RepairOrderVO order;

    // 报修人可见的重复报修概括提醒
    private String duplicateReminder;
}
