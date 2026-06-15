package com.campus.system.service.impl;

import com.campus.system.common.enums.AssetStatus;
import com.campus.system.common.enums.RepairerAcceptingState;
import com.campus.system.common.enums.StatisticsRangeType;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.dto.StatisticsQueryDTO;
import com.campus.system.entity.SysUser;
import com.campus.system.mapper.ManagementStatisticsMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.service.interfaces.ManagementStatisticsService;
import com.campus.system.vo.AssetRepairRiskItemVO;
import com.campus.system.vo.ManagementStatisticsVO;
import com.campus.system.vo.RepairEfficiencyStatVO;
import com.campus.system.vo.RepairerWorkStatVO;
import com.campus.system.vo.StatisticsDistributionItemVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 管理统计业务实现
@Service
public class ManagementStatisticsServiceImpl implements ManagementStatisticsService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TREND_DAY = DateTimeFormatter.ofPattern("MM-dd");

    @Autowired
    private ManagementStatisticsMapper statisticsMapper;

    @Autowired
    private SysUserMapper users;

    @Autowired
    private BusinessClock clock;

    @Override
    public ManagementStatisticsVO overview(StatisticsQueryDTO query) {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可查看管理统计");
        }
        StatisticsRangeType rangeType = StatisticsRangeType.of(query == null ? null : query.getRangeType());
        LocalDateTime[] range = resolveRange(rangeType);
        ManagementStatisticsVO vo = new ManagementStatisticsVO();
        vo.setRangeType(rangeType.getCode());
        vo.setRangeTypeLabel(rangeType.getLabel());
        vo.setRangeStart(range[0].format(DISPLAY_TIME));
        vo.setRangeEnd(range[1].format(DISPLAY_TIME));
        RepairEfficiencyStatVO efficiency = statisticsMapper.repairEfficiency(range[0], range[1]);
        if (efficiency == null) {
            efficiency = new RepairEfficiencyStatVO();
        }
        Long unfinished = statisticsMapper.countUnfinishedSnapshot();
        efficiency.setUnfinishedCount(unfinished == null ? 0L : unfinished);
        vo.setRepairEfficiency(efficiency);
        List<AssetRepairRiskItemVO> topRepaired = statisticsMapper.topRepairedAssets(range[0], range[1], 10);
        topRepaired.forEach(this::enrichTopRepairedAsset);
        vo.setTopRepairedAssets(topRepaired);
        vo.setAssetCategoryRepairs(statisticsMapper.assetCategoryRepairs(range[0], range[1]));
        vo.setUnfinishedOrderTrend(buildUnfinishedOrderTrend(range[0], range[1]));
        return vo;
    }

    private List<StatisticsDistributionItemVO> buildUnfinishedOrderTrend(LocalDateTime start, LocalDateTime end) {
        List<StatisticsDistributionItemVO> trend = new ArrayList<>();
        LocalDate day = start.toLocalDate();
        LocalDate lastDay = end.toLocalDate().minusDays(1);
        while (!day.isAfter(lastDay)) {
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
            Long count = statisticsMapper.countUnfinishedAt(dayEnd);
            StatisticsDistributionItemVO item = new StatisticsDistributionItemVO();
            item.setName(day.format(TREND_DAY));
            item.setCount(count == null ? 0L : count);
            trend.add(item);
            day = day.plusDays(1);
        }
        return trend;
    }

    @Override
    public RepairerWorkStatVO personal(StatisticsQueryDTO query) {
        if (!"REPAIRER".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅维修师傅可查看个人统计");
        }
        StatisticsRangeType rangeType = StatisticsRangeType.of(query == null ? null : query.getRangeType());
        LocalDateTime[] range = resolveRange(rangeType);
        RepairerWorkStatVO stat = statisticsMapper.repairerPersonalStats(SecurityUtils.current().userId(), range[0], range[1]);
        if (stat == null) {
            stat = new RepairerWorkStatVO();
            stat.setRepairerId(SecurityUtils.current().userId());
        }
        SysUser repairer = users.selectById(SecurityUtils.current().userId());
        if (repairer != null) {
            stat.setUserNo(repairer.getUserNo());
            stat.setRealName(repairer.getRealName());
            String state = repairer.getAcceptingState();
            stat.setAcceptingState(state == null || state.isBlank() ? RepairerAcceptingState.AVAILABLE.getCode() : state);
            stat.setPauseReason(repairer.getPauseReason());
            stat.setExpectedResumeTime(repairer.getExpectedResumeTime());
        }
        enrichRepairerState(stat);
        return stat;
    }

    private LocalDateTime[] resolveRange(StatisticsRangeType rangeType) {
        LocalDate today = clock.today();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        if (rangeType == StatisticsRangeType.CURRENT_YEAR) {
            LocalDateTime start = LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
            return new LocalDateTime[] {start, end};
        }
        LocalDateTime start = today.minusDays(rangeType.getDays() - 1L).atStartOfDay();
        return new LocalDateTime[] {start, end};
    }

    private void enrichTopRepairedAsset(AssetRepairRiskItemVO item) {
        if (item == null) {
            return;
        }
        enrichPurchaseAge(item);
        if (item.getStatus() == null || item.getStatus().isBlank()) {
            return;
        }
        try {
            item.setStatusLabel(AssetStatus.of(item.getStatus()).getLabel());
        } catch (Exception ignored) {
            item.setStatusLabel(item.getStatus());
        }
    }

    private void enrichPurchaseAge(AssetRepairRiskItemVO item) {
        if (item == null || item.getPurchaseDate() == null) {
            return;
        }
        long months = ChronoUnit.MONTHS.between(item.getPurchaseDate(), clock.today());
        if (months < 0) {
            months = 0;
        }
        item.setPurchaseYears((int) (months / 12));
        item.setPurchaseMonths((int) (months % 12));
    }

    private void enrichRepairerState(RepairerWorkStatVO stat) {
        if (stat == null || stat.getAcceptingState() == null || stat.getAcceptingState().isBlank()) {
            if (stat != null) {
                stat.setAcceptingState(RepairerAcceptingState.AVAILABLE.getCode());
                stat.setAcceptingStateLabel(RepairerAcceptingState.AVAILABLE.getLabel());
            }
            return;
        }
        stat.setAcceptingStateLabel(RepairerAcceptingState.of(stat.getAcceptingState()).getLabel());
    }
}
