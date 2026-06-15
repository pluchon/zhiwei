package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 维修师傅接单状态响应数据
@Data
public class RepairerAvailabilityVO {

    // 维修师傅用户主键
    private Long repairerId;

    // 维修师傅姓名
    private String repairerRealName;

    // 接单可用状态
    private String acceptingState;

    // 接单状态中文名称
    private String acceptingStateLabel;

    // 暂停接单原因
    private String pauseReason;

    // 预计恢复接单时间
    private LocalDateTime expectedResumeTime;
}
