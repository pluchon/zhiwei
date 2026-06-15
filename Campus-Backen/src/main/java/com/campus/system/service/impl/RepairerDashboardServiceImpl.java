package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.enums.RepairerAcceptingState;
import com.campus.system.common.enums.RepairStatus;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.security.CurrentUser;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.entity.RepairCategory;
import com.campus.system.entity.RepairOrder;
import com.campus.system.entity.SysUser;
import com.campus.system.mapper.ManagementStatisticsMapper;
import com.campus.system.mapper.RepairCategoryMapper;
import com.campus.system.mapper.RepairOrderMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.service.interfaces.RepairerDashboardService;
import com.campus.system.vo.RepairerDashboardVO;
import com.campus.system.vo.RepairerWorkStatVO;
import com.campus.system.vo.ReporterDashboardRecentOrderVO;
import com.campus.system.vo.StatisticsDistributionItemVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 维修师傅首页看板实现
@Service
public class RepairerDashboardServiceImpl implements RepairerDashboardService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("M/d");

    @Autowired
    private RepairOrderMapper orders;

    @Autowired
    private RepairCategoryMapper categories;

    @Autowired
    private ManagementStatisticsMapper statisticsMapper;

    @Autowired
    private SysUserMapper users;

    @Autowired
    private BusinessClock clock;

    @Override
    public RepairerDashboardVO dashboard(Integer rangeDays) {
        CurrentUser me = SecurityUtils.current();
        if (!"REPAIRER".equals(me.roleCode())) {
            throw BusinessException.forbidden("当前角色不可查看维修首页");
        }
        int days = normalizeRangeDays(rangeDays);
        Long userId = me.userId();
        LocalDateTime rangeStart = clock.now().minusDays(days);
        LocalDateTime[] range = resolveRange(days);

        List<RepairOrder> allMine = orders.selectList(Wrappers.<RepairOrder>lambdaQuery()
                .eq(RepairOrder::getDeleteState, 0).eq(RepairOrder::getCurrentRepairerId, userId).orderByDesc(RepairOrder::getCreateTime));
        RepairerDashboardVO vo = new RepairerDashboardVO();
        vo.setRangeDays(days);
        vo.setInProgress(countByStatuses(allMine, RepairStatus.ACCEPTED.getCode(), RepairStatus.PROCESSING.getCode()));
        vo.setPendingConfirm(countByStatuses(allMine, RepairStatus.PENDING_CONFIRM.getCode()));
        vo.setCompleted(countByStatuses(allMine, RepairStatus.COMPLETED.getCode(), RepairStatus.CLOSED.getCode()));
        vo.setWorkStat(loadWorkStat(userId, range[0], range[1]));
        vo.setStatusDistribution(buildStatusDistribution(allMine));
        vo.setFaultTypeDistribution(buildFaultTypeDistribution(allMine, rangeStart));
        vo.setCompletionTrend(buildCompletionTrend(allMine, days));
        vo.setRecentOrders(buildRecentOrders(allMine));
        return vo;
    }

    private RepairerWorkStatVO loadWorkStat(Long userId, LocalDateTime start, LocalDateTime end) {
        RepairerWorkStatVO stat = statisticsMapper.repairerPersonalStats(userId, start, end);
        if (stat == null) {
            stat = new RepairerWorkStatVO();
            stat.setRepairerId(userId);
        }
        SysUser repairer = users.selectById(userId);
        if (repairer != null) {
            stat.setUserNo(repairer.getUserNo());
            stat.setRealName(repairer.getRealName());
            String state = repairer.getAcceptingState();
            stat.setAcceptingState(state == null || state.isBlank() ? RepairerAcceptingState.AVAILABLE.getCode() : state);
            stat.setPauseReason(repairer.getPauseReason());
            stat.setExpectedResumeTime(repairer.getExpectedResumeTime());
            stat.setAcceptingStateLabel(RepairerAcceptingState.of(stat.getAcceptingState()).getLabel());
        }
        return stat;
    }

    private LocalDateTime[] resolveRange(int days) {
        LocalDate today = clock.today();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        LocalDateTime start = today.minusDays(days - 1L).atStartOfDay();
        return new LocalDateTime[] {start, end};
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

    private long countByStatuses(List<RepairOrder> items, int... statuses) {
        long total = 0;
        for (int status : statuses) {
            total += items.stream().filter(order -> order.getStatus() != null && order.getStatus() == status).count();
        }
        return total;
    }

    private List<StatisticsDistributionItemVO> buildStatusDistribution(List<RepairOrder> items) {
        Map<Integer, Long> counts = new HashMap<>();
        for (RepairOrder order : items) {
            if (order.getStatus() == null) {
                continue;
            }
            counts.merge(order.getStatus(), 1L, Long::sum);
        }
        List<StatisticsDistributionItemVO> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            StatisticsDistributionItemVO item = new StatisticsDistributionItemVO();
            item.setName(statusLabel(entry.getKey()));
            item.setCount(entry.getValue());
            result.add(item);
        }
        result.sort(Comparator.comparingLong(StatisticsDistributionItemVO::getCount).reversed());
        return result;
    }

    private List<StatisticsDistributionItemVO> buildFaultTypeDistribution(List<RepairOrder> items, LocalDateTime rangeStart) {
        Map<Long, Long> counts = new HashMap<>();
        for (RepairOrder order : items) {
            if (order.getCreateTime() == null || order.getCreateTime().isBefore(rangeStart)) {
                continue;
            }
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

    private List<StatisticsDistributionItemVO> buildCompletionTrend(List<RepairOrder> items, int days) {
        LocalDate today = clock.today();
        LocalDate startDay = today.minusDays(days - 1L);
        Map<LocalDate, Long> counts = new HashMap<>();
        for (RepairOrder order : items) {
            if (order.getCompletionTime() == null) {
                continue;
            }
            if (order.getStatus() == null || order.getStatus() != RepairStatus.COMPLETED.getCode()) {
                continue;
            }
            LocalDate day = order.getCompletionTime().toLocalDate();
            if (day.isBefore(startDay) || day.isAfter(today)) {
                continue;
            }
            counts.merge(day, 1L, Long::sum);
        }
        List<StatisticsDistributionItemVO> result = new ArrayList<>();
        for (LocalDate day = startDay; !day.isAfter(today); day = day.plusDays(1)) {
            StatisticsDistributionItemVO item = new StatisticsDistributionItemVO();
            item.setName(day.format(DAY_LABEL));
            item.setCount(counts.getOrDefault(day, 0L));
            result.add(item);
        }
        return result;
    }

    private List<ReporterDashboardRecentOrderVO> buildRecentOrders(List<RepairOrder> items) {
        List<ReporterDashboardRecentOrderVO> result = new ArrayList<>();
        int limit = Math.min(items.size(), 5);
        for (int i = 0; i < limit; i++) {
            RepairOrder order = items.get(i);
            ReporterDashboardRecentOrderVO vo = new ReporterDashboardRecentOrderVO();
            vo.setOrderId(order.getOrderId());
            vo.setTitle(order.getTitle());
            vo.setStatus(order.getStatus());
            vo.setStatusLabel(statusLabel(order.getStatus()));
            vo.setCreateTime(order.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "待匹配";
            case 2 -> "待接单";
            case 3 -> "已接单";
            case 4 -> "处理中";
            case 5 -> "待确认";
            case 6 -> "待仲裁";
            case 7 -> "已完成";
            case 8 -> "已驳回";
            case 9 -> "已关闭";
            default -> "未知状态";
        };
    }
}
