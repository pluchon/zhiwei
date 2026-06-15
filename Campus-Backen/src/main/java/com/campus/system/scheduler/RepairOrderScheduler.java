package com.campus.system.scheduler;

import com.campus.system.service.interfaces.AttachmentCleanupService;
import com.campus.system.service.interfaces.RepairOrderTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 报修相关定时任务调度器
@Component
public class RepairOrderScheduler {

    @Autowired
    private RepairOrderTaskService taskService;

    @Autowired
    private AttachmentCleanupService attachmentCleanupService;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Shanghai")
    public void hourlyTasks() {
        taskService.processWorkCycleReminders();
        taskService.processConfirmationReminders();
        taskService.processAutoComplete();
        taskService.processDraftCleanup();
        attachmentCleanupService.processCleanup();
    }
}
