package com.campus.system.service.interfaces;

import java.util.List;

/**
 * 维修周期与待确认周期业务接口。
 */
public interface RepairCycleService {

    void startWorkCycle(Long orderId, Long repairerId);

    void endWorkCycle(Long orderId);

    void startConfirmationCycle(Long orderId, Long reporterId);

    void endConfirmationCycle(Long orderId);

    boolean isLongStagnant(Long orderId);

    // 查询当前满足长时间未进展条件的工单主键列表。
    List<Long> findLongStagnantOrderIds();
}
