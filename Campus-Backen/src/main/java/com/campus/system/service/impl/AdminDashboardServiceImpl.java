package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.enums.RepairStatus;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.entity.RepairCategory;
import com.campus.system.entity.RepairOrder;
import com.campus.system.mapper.RepairCategoryMapper;
import com.campus.system.mapper.RepairOrderMapper;
import com.campus.system.service.interfaces.AdminDashboardService;
import com.campus.system.service.interfaces.RepairCycleService;
import com.campus.system.vo.RepairDashboardVO;
import com.campus.system.vo.StatisticsDistributionItemVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 管理员报修看板统计实现
@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    @Autowired
    private RepairOrderMapper orders;

    @Autowired
    private RepairCategoryMapper categories;

    @Autowired
    private RepairCycleService repairCycleService;

    @Autowired
    private BusinessClock clock;

    @Override
    public RepairDashboardVO dashboard(Integer rangeDays) {
        int days = normalizeRangeDays(rangeDays);
        RepairDashboardVO vo = new RepairDashboardVO();
        vo.setRangeDays(days);
        vo.setPendingDispatch(countStatus(RepairStatus.PENDING_DISPATCH.getCode()));
        vo.setPendingAccept(countStatus(RepairStatus.PENDING_ACCEPT.getCode()));
        vo.setAccepted(countStatus(RepairStatus.ACCEPTED.getCode()));
        vo.setProcessing(countStatus(RepairStatus.PROCESSING.getCode()));
        vo.setPendingConfirm(countStatus(RepairStatus.PENDING_CONFIRM.getCode()));
        vo.setPendingArbitration(countStatus(RepairStatus.PENDING_ARBITRATION.getCode()));
        vo.setLongStagnant(countLongStagnant());
        LocalDateTime start = clock.today().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        vo.setTodayCreated(orders.selectCount(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getDeleteState, 0)
                .ge(RepairOrder::getCreateTime, start).lt(RepairOrder::getCreateTime, end)));
        vo.setTodayCompleted(orders.selectCount(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getDeleteState, 0)
                .isNotNull(RepairOrder::getCompletionTime).ge(RepairOrder::getCompletionTime, start).lt(RepairOrder::getCompletionTime, end)));
        LocalDateTime rangeStart = clock.now().minusDays(days);
        vo.setFaultTypeDistribution(buildFaultTypeDistribution(rangeStart));
        vo.setCampusDistribution(buildCampusDistribution(rangeStart));
        vo.setBuildingDistribution(buildBuildingDistribution(rangeStart));
        vo.setCurrentStatusDistribution(buildCurrentStatusDistribution());
        return vo;
    }

    private int normalizeRangeDays(Integer rangeDays) {
        if (rangeDays == null) {
            return 30;
        }
        if (rangeDays == 7 || rangeDays == 30 || rangeDays == 90) {
            return rangeDays;
        }
        return 30;
    }

    private List<StatisticsDistributionItemVO> buildFaultTypeDistribution(LocalDateTime rangeStart) {
        List<RepairOrder> items = orders.selectList(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getDeleteState, 0)
                .ge(RepairOrder::getCreateTime, rangeStart));
        Map<Long, Long> counts = new HashMap<>();
        for (RepairOrder order : items) {
            counts.merge(order.getCategoryId(), 1L, Long::sum);
        }
        List<StatisticsDistributionItemVO> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : counts.entrySet()) {
            RepairCategory category = categories.selectById(entry.getKey());
            StatisticsDistributionItemVO item = new StatisticsDistributionItemVO();
            item.setName(category != null ? category.getCategoryName() : "未知类型");
            item.setCount(entry.getValue());
            result.add(item);
        }
        result.sort(Comparator.comparingLong(StatisticsDistributionItemVO::getCount).reversed());
        return result;
    }

    private List<StatisticsDistributionItemVO> buildCampusDistribution(LocalDateTime rangeStart) {
        List<RepairOrder> items = orders.selectList(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getDeleteState, 0).ge(RepairOrder::getCreateTime, rangeStart));
        Map<String, Long> counts = new HashMap<>();
        for (RepairOrder order : items) {
            String name = order.getCampus() == null ? "未知校区" : order.getCampus();
            counts.merge(name, 1L, Long::sum);
        }
        return toSortedDistribution(counts);
    }

    private List<StatisticsDistributionItemVO> buildBuildingDistribution(LocalDateTime rangeStart) {
        List<RepairOrder> items = orders.selectList(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getDeleteState, 0).ge(RepairOrder::getCreateTime, rangeStart));
        Map<String, Long> counts = new HashMap<>();
        for (RepairOrder order : items) {
            String campus = order.getCampus() == null ? "未知校区" : order.getCampus();
            String building = order.getBuilding() == null || order.getBuilding().isBlank() ? "未指定楼栋" : order.getBuilding();
            counts.merge(campus + " " + building, 1L, Long::sum);
        }
        List<StatisticsDistributionItemVO> sorted = toSortedDistribution(counts);
        if (sorted.size() <= 10) {
            return sorted;
        }
        List<StatisticsDistributionItemVO> top = new ArrayList<>(sorted.subList(0, 10));
        long otherCount = sorted.subList(10, sorted.size()).stream().mapToLong(StatisticsDistributionItemVO::getCount).sum();
        StatisticsDistributionItemVO other = new StatisticsDistributionItemVO();
        other.setName("其他");
        other.setCount(otherCount);
        top.add(other);
        return top;
    }

    private List<StatisticsDistributionItemVO> buildCurrentStatusDistribution() {
        List<StatisticsDistributionItemVO> result = new ArrayList<>();
        for (RepairStatus status : RepairStatus.values()) {
            StatisticsDistributionItemVO item = new StatisticsDistributionItemVO();
            item.setName(String.valueOf(status.getCode()));
            item.setCount(countStatus(status.getCode()));
            result.add(item);
        }
        return result;
    }

    private List<StatisticsDistributionItemVO> toSortedDistribution(Map<String, Long> counts) {
        List<StatisticsDistributionItemVO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            StatisticsDistributionItemVO item = new StatisticsDistributionItemVO();
            item.setName(entry.getKey());
            item.setCount(entry.getValue());
            result.add(item);
        }
        result.sort(Comparator.comparingLong(StatisticsDistributionItemVO::getCount).reversed());
        return result;
    }

    private long countStatus(int status) {
        return orders.selectCount(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getDeleteState, 0).eq(RepairOrder::getStatus, status));
    }

    private long countLongStagnant() {
        List<Long> orderIds = repairCycleService.findLongStagnantOrderIds();
        if (orderIds.isEmpty()) {
            return 0;
        }
        return orders.selectCount(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getDeleteState, 0).in(RepairOrder::getStatus,
                RepairStatus.ACCEPTED.getCode(), RepairStatus.PROCESSING.getCode()).in(RepairOrder::getOrderId, orderIds));
    }
}
