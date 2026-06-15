package com.campus.system.dto;

import java.time.LocalDateTime;
import lombok.Data;

// 维修师傅接单状态更新请求参数
@Data
public class RepairerAvailabilityUpdateDTO {

    // 接单状态（AVAILABLE可接单 PAUSED暂停接单）
    private String acceptingState;

    // 暂停接单原因
    private String pauseReason;

    // 预计恢复接单时间
    private LocalDateTime expectedResumeTime;
}
