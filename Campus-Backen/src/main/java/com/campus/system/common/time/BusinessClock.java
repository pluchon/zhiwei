package com.campus.system.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

// 业务时钟接口
public interface BusinessClock {

    ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    LocalDateTime now();

    default LocalDate today() {
        return now().atZone(ZONE).toLocalDate();
    }
}
