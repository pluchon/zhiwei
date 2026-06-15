package com.campus.system.scheduler;

import com.campus.system.service.interfaces.ManualAccountRecoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 人工恢复申请过期定时任务
@Component
public class ManualAccountRecoveryExpireTask {

    @Autowired
    private ManualAccountRecoveryService recoveryService;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Shanghai")
    public void expireApproved() {
        recoveryService.expireApprovedRecords();
    }
}
