package com.campus.system.common.time;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

// 默认业务时钟
@Component
public class SystemBusinessClock implements BusinessClock {

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now(BusinessClock.ZONE);
    }
}
