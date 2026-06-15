package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.enums.RepairStatus;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.entity.Campus;
import com.campus.system.entity.RepairCategory;
import com.campus.system.entity.RepairOrder;
import com.campus.system.mapper.CampusMapper;
import com.campus.system.mapper.RepairCategoryMapper;
import com.campus.system.mapper.RepairOrderMapper;
import com.campus.system.service.interfaces.PortalSummaryService;
import com.campus.system.vo.PortalSummaryVO;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PortalSummaryServiceImpl implements PortalSummaryService {

    @Autowired
    private RepairOrderMapper orders;

    @Autowired
    private RepairCategoryMapper categories;

    @Autowired
    private CampusMapper campuses;

    @Autowired
    private BusinessClock clock;

    @Override
    public PortalSummaryVO summary() {
        LocalDateTime start = clock.today().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        PortalSummaryVO vo = new PortalSummaryVO();
        vo.setTodayOrders(orders.selectCount(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getDeleteState, 0).ge(RepairOrder::getCreateTime, start).lt(RepairOrder::getCreateTime, end)));
        vo.setProcessingOrders(orders.selectCount(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getDeleteState, 0)
                .in(RepairOrder::getStatus, RepairStatus.ACCEPTED.getCode(), RepairStatus.PROCESSING.getCode(), RepairStatus.PENDING_CONFIRM.getCode())));
        vo.setCategoryCount(categories.selectCount(Wrappers.<RepairCategory>lambdaQuery().eq(RepairCategory::getDeleteState, 0).eq(RepairCategory::getStatus, 0)));
        vo.setCampusCount(campuses.selectCount(Wrappers.<Campus>lambdaQuery().eq(Campus::getDeleteState, 0).eq(Campus::getStatus, 0)));
        return vo;
    }
}
