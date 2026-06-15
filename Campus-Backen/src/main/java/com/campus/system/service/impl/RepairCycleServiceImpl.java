package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.entity.RepairConfirmationCycle;
import com.campus.system.entity.RepairWorkCycle;
import com.campus.system.mapper.RepairConfirmationCycleMapper;
import com.campus.system.mapper.RepairWorkCycleMapper;
import com.campus.system.service.interfaces.RepairCycleService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 维修周期与待确认周期
@Service
public class RepairCycleServiceImpl implements RepairCycleService {

    @Autowired
    private RepairWorkCycleMapper workCycles;

    @Autowired
    private RepairConfirmationCycleMapper confirmationCycles;

    @Autowired
    private BusinessClock clock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startWorkCycle(Long orderId, Long repairerId) {
        endWorkCycle(orderId);
        RepairWorkCycle cycle = new RepairWorkCycle();
        cycle.setOrderId(orderId);
        cycle.setRepairerId(repairerId);
        cycle.setStartTime(clock.now());
        cycle.setActiveFlag(1);
        cycle.setThreeDayReminded(0);
        cycle.setSevenDayReminded(0);
        workCycles.insert(cycle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endWorkCycle(Long orderId) {
        workCycles.update(null, Wrappers.<RepairWorkCycle>lambdaUpdate().eq(RepairWorkCycle::getOrderId, orderId)
                .eq(RepairWorkCycle::getActiveFlag, 1).set(RepairWorkCycle::getEndTime, clock.now()).set(RepairWorkCycle::getActiveFlag, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startConfirmationCycle(Long orderId, Long reporterId) {
        endConfirmationCycle(orderId);
        RepairConfirmationCycle cycle = new RepairConfirmationCycle();
        cycle.setOrderId(orderId);
        cycle.setReporterId(reporterId);
        cycle.setStartTime(clock.now());
        cycle.setActiveFlag(1);
        cycle.setThreeDayReminded(0);
        cycle.setSevenDayReminded(0);
        cycle.setTwentySevenDayReminded(0);
        cycle.setAutoCompleted(0);
        confirmationCycles.insert(cycle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endConfirmationCycle(Long orderId) {
        confirmationCycles.update(null, Wrappers.<RepairConfirmationCycle>lambdaUpdate().eq(RepairConfirmationCycle::getOrderId, orderId).eq(RepairConfirmationCycle::getActiveFlag, 1)
                .set(RepairConfirmationCycle::getEndTime, clock.now()).set(RepairConfirmationCycle::getActiveFlag, null));
    }

    @Override
    public boolean isLongStagnant(Long orderId) {
      RepairWorkCycle active = activeWorkCycle(orderId);
      return active != null && active.getSevenDayReminded() != null && active.getSevenDayReminded() == 1;
    }

    @Override
    public List<Long> findLongStagnantOrderIds() {
      return workCycles.selectList(Wrappers.<RepairWorkCycle>lambdaQuery().eq(RepairWorkCycle::getActiveFlag, 1)
                  .eq(RepairWorkCycle::getSevenDayReminded, 1).isNull(RepairWorkCycle::getEndTime).select(RepairWorkCycle::getOrderId))
          .stream().map(RepairWorkCycle::getOrderId).distinct().toList();
    }

    private RepairWorkCycle activeWorkCycle(Long orderId) {
      return workCycles.selectOne(Wrappers.<RepairWorkCycle>lambdaQuery().eq(RepairWorkCycle::getOrderId, orderId)
              .eq(RepairWorkCycle::getActiveFlag, 1).isNull(RepairWorkCycle::getEndTime).last("LIMIT 1"));
    }
}
