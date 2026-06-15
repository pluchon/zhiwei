package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 维修师傅工作统计响应数据
@Data
public class RepairerWorkStatVO {

    // 维修师傅用户主键
    private Long repairerId;

    // 维修师傅账号
    private String userNo;

    // 维修师傅姓名
    private String realName;

    // 接单可用状态
    private String acceptingState;

    // 接单状态中文名称
    private String acceptingStateLabel;

    // 暂停接单原因
    private String pauseReason;

    // 预计恢复接单时间
    private LocalDateTime expectedResumeTime;

    // 接单数量
    private Long acceptCount;

    // 完成工单数量
    private Long completedCount;

    // 当前处理中工单数量
    private Long processingCount;

    // 平均首次处理时长（分钟）
    private Double avgFirstProcessMinutes;

    // 平均完成时长（分钟）
    private Double avgCompletionMinutes;
}
