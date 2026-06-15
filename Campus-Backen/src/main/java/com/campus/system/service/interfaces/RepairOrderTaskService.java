package com.campus.system.service.interfaces;

/**
 * 报修定时任务业务接口。
 */
public interface RepairOrderTaskService {

  void processWorkCycleReminders();

  void processConfirmationReminders();

  void processAutoComplete();

  void processDraftCleanup();
}
