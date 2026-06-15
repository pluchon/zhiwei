package com.campus.system.service.impl;

import com.campus.system.common.enums.AiChartDimension;
import com.campus.system.common.enums.AiChartType;
import com.campus.system.common.enums.StatisticsRangeType;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.dto.StatisticsQueryDTO;
import com.campus.system.service.interfaces.AdminDashboardService;
import com.campus.system.service.interfaces.AiDynamicChartService;
import com.campus.system.service.interfaces.ManagementStatisticsService;
import com.campus.system.service.interfaces.RepairerDashboardService;
import com.campus.system.vo.AiDynamicChartVO;
import com.campus.system.vo.AssetCategoryRepairStatVO;
import com.campus.system.vo.AssetRepairRiskItemVO;
import com.campus.system.vo.ManagementStatisticsVO;
import com.campus.system.vo.RepairDashboardVO;
import com.campus.system.vo.RepairEfficiencyStatVO;
import com.campus.system.vo.RepairerDashboardVO;
import com.campus.system.vo.RepairerWorkStatVO;
import com.campus.system.vo.StatisticsDistributionItemVO;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// AI 助手动态图表：AI 选维度与图表类型，数据由后端真实查询
@Service
public class AiDynamicChartServiceImpl implements AiDynamicChartService {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private ManagementStatisticsService statisticsService;

    @Autowired
    private RepairerDashboardService repairerDashboardService;

    @Autowired
    private BusinessClock clock;

    @Override
    public List<AiDynamicChartVO> buildCharts(String role, String rangeType, String chartTypeCode,
            String dimensionCode, String message) {
        StatisticsRangeType range = StatisticsRangeType.of(rangeType);
        AiChartDimension dimension = AiChartDimension.resolveForRole(role, dimensionCode, message);
        AiChartType chartType = resolveChartType(chartTypeCode, message, dimension);
        AiDynamicChartVO chart = new AiDynamicChartVO();
        chart.setDimension(dimension.getCode());
        chart.setChartType(chartType.getCode());
        chart.setRangeLabel(buildRangeLabel(range, dimension));
        chart.setTitle(buildTitle(range, dimension));
        chart.setUnit(resolveUnit(dimension));
        chart.setHorizontal(Boolean.FALSE);
        chart.setItems(loadItems(role, range, dimension));
        if (dimension == AiChartDimension.TOP_REPAIRED_ASSETS) {
            chart.setHorizontal(Boolean.TRUE);
        }
        normalizeChartType(chart, dimension);
        return List.of(chart);
    }

    private AiChartType resolveChartType(String chartTypeCode, String message, AiChartDimension dimension) {
        AiChartType chartType;
        if (chartTypeCode == null || chartTypeCode.isBlank()) {
            chartType = AiChartType.fromMessage(message);
        } else {
            try {
                chartType = AiChartType.of(chartTypeCode);
            } catch (Exception ex) {
                chartType = AiChartType.fromMessage(message);
            }
        }
        if (dimension.isTrendOnly()) {
            return AiChartType.LINE;
        }
        if (dimension == AiChartDimension.REPAIR_EFFICIENCY) {
            return AiChartType.BAR;
        }
        return chartType;
    }

    private void normalizeChartType(AiDynamicChartVO chart, AiChartDimension dimension) {
        if (dimension.isTrendOnly()) {
            chart.setChartType(AiChartType.LINE.getCode());
            return;
        }
        if (dimension == AiChartDimension.REPAIR_EFFICIENCY) {
            chart.setChartType(AiChartType.BAR.getCode());
            return;
        }
        List<StatisticsDistributionItemVO> items = chart.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        if (AiChartType.PIE.getCode().equals(chart.getChartType()) && items.size() > 8) {
            chart.setChartType(AiChartType.BAR.getCode());
        }
    }

    private List<StatisticsDistributionItemVO> loadItems(String role, StatisticsRangeType range,
            AiChartDimension dimension) {
        if ("ADMIN".equals(role)) {
            return loadAdminItems(range, dimension);
        }
        return loadRepairerItems(range, dimension);
    }

    private List<StatisticsDistributionItemVO> loadAdminItems(StatisticsRangeType range, AiChartDimension dimension) {
        int rangeDays = toDashboardRangeDays(range);
        RepairDashboardVO dashboard = adminDashboardService.dashboard(rangeDays);
        return switch (dimension) {
            case FAULT_TYPE -> dashboard.getFaultTypeDistribution();
            case CAMPUS -> dashboard.getCampusDistribution();
            case BUILDING -> dashboard.getBuildingDistribution();
            case ORDER_STATUS -> normalizeStatusItems(dashboard.getCurrentStatusDistribution());
            case ASSET_CATEGORY -> toCategoryItems(loadOverview(range).getAssetCategoryRepairs());
            case UNFINISHED_TREND -> loadOverview(range).getUnfinishedOrderTrend();
            case TOP_REPAIRED_ASSETS -> toTopAssetItems(loadOverview(range).getTopRepairedAssets());
            case REPAIR_EFFICIENCY -> toEfficiencyItems(loadOverview(range).getRepairEfficiency());
            default -> List.of();
        };
    }

    private List<StatisticsDistributionItemVO> loadRepairerItems(StatisticsRangeType range, AiChartDimension dimension) {
        int rangeDays = toDashboardRangeDays(range);
        RepairerDashboardVO dashboard = repairerDashboardService.dashboard(rangeDays);
        return switch (dimension) {
            case REPAIRER_STATUS -> dashboard.getStatusDistribution();
            case REPAIRER_FAULT_TYPE -> dashboard.getFaultTypeDistribution();
            case REPAIRER_COMPLETION_TREND -> dashboard.getCompletionTrend();
            case REPAIRER_WORK_SUMMARY -> toWorkSummaryItems(dashboard);
            default -> List.of();
        };
    }

    private ManagementStatisticsVO loadOverview(StatisticsRangeType range) {
        StatisticsQueryDTO query = new StatisticsQueryDTO();
        query.setRangeType(range.getCode());
        return statisticsService.overview(query);
    }

    private List<StatisticsDistributionItemVO> normalizeStatusItems(List<StatisticsDistributionItemVO> items) {
        if (items == null) {
            return List.of();
        }
        List<StatisticsDistributionItemVO> result = new ArrayList<>();
        for (StatisticsDistributionItemVO item : items) {
            if (item.getCount() <= 0) {
                continue;
            }
            StatisticsDistributionItemVO row = new StatisticsDistributionItemVO();
            row.setName(resolveStatusLabel(item.getName()));
            row.setCount(item.getCount());
            result.add(row);
        }
        return result;
    }

    private List<StatisticsDistributionItemVO> toCategoryItems(List<AssetCategoryRepairStatVO> rows) {
        if (rows == null) {
            return List.of();
        }
        List<StatisticsDistributionItemVO> result = new ArrayList<>();
        for (AssetCategoryRepairStatVO row : rows) {
            StatisticsDistributionItemVO item = new StatisticsDistributionItemVO();
            item.setName(row.getCategoryName() == null ? "未分类" : row.getCategoryName());
            item.setCount(row.getRepairCount() == null ? 0L : row.getRepairCount());
            result.add(item);
        }
        return result;
    }

    private List<StatisticsDistributionItemVO> toTopAssetItems(List<AssetRepairRiskItemVO> rows) {
        if (rows == null) {
            return List.of();
        }
        List<StatisticsDistributionItemVO> result = new ArrayList<>();
        for (AssetRepairRiskItemVO row : rows) {
            StatisticsDistributionItemVO item = new StatisticsDistributionItemVO();
            String name = row.getAssetName();
            if (name == null || name.isBlank()) {
                name = row.getAssetNo() == null ? "未知资产" : row.getAssetNo();
            }
            item.setName(name);
            item.setCount(row.getRepairCount() == null ? 0L : row.getRepairCount());
            result.add(item);
        }
        return result;
    }

    private List<StatisticsDistributionItemVO> toEfficiencyItems(RepairEfficiencyStatVO efficiency) {
        List<StatisticsDistributionItemVO> result = new ArrayList<>();
        if (efficiency == null) {
            return result;
        }
        addMetricItem(result, "平均首次接单(分钟)", efficiency.getAvgFirstAcceptMinutes());
        addMetricItem(result, "平均处理(分钟)", efficiency.getAvgProcessMinutes());
        addMetricItem(result, "平均完成(分钟)", efficiency.getAvgCompletionMinutes());
        addCountItem(result, "超7天完成", efficiency.getOverSevenDaysCount());
        addCountItem(result, "当前未完成", efficiency.getUnfinishedCount());
        return result;
    }

    private List<StatisticsDistributionItemVO> toWorkSummaryItems(RepairerDashboardVO dashboard) {
        List<StatisticsDistributionItemVO> result = new ArrayList<>();
        RepairerWorkStatVO stat = dashboard.getWorkStat();
        addCountItem(result, "接单数", stat == null ? 0L : stat.getAcceptCount());
        addCountItem(result, "完成数", stat == null ? 0L : stat.getCompletedCount());
        addCountItem(result, "处理中", stat == null ? 0L : stat.getProcessingCount());
        addCountItem(result, "待确认", dashboard.getPendingConfirm());
        return result;
    }

    private void addMetricItem(List<StatisticsDistributionItemVO> result, String name, Double value) {
        StatisticsDistributionItemVO item = new StatisticsDistributionItemVO();
        item.setName(name);
        item.setCount(value == null ? 0L : Math.round(value));
        result.add(item);
    }

    private void addCountItem(List<StatisticsDistributionItemVO> result, String name, Long value) {
        StatisticsDistributionItemVO item = new StatisticsDistributionItemVO();
        item.setName(name);
        item.setCount(value == null ? 0L : value);
        result.add(item);
    }

    private String buildTitle(StatisticsRangeType range, AiChartDimension dimension) {
        if (dimension == AiChartDimension.ORDER_STATUS) {
            return dimension.getLabel();
        }
        return range.getLabel() + " · " + dimension.getLabel();
    }

    private String buildRangeLabel(StatisticsRangeType range, AiChartDimension dimension) {
        if (dimension == AiChartDimension.ORDER_STATUS) {
            return "当前快照";
        }
        return range.getLabel();
    }

    private String resolveUnit(AiChartDimension dimension) {
        if (dimension == AiChartDimension.REPAIR_EFFICIENCY) {
            return "分钟/单";
        }
        return "工单数";
    }

    private int toDashboardRangeDays(StatisticsRangeType range) {
        if (range == StatisticsRangeType.CURRENT_YEAR) {
            LocalDate today = clock.today();
            LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
            int days = (int) ChronoUnit.DAYS.between(yearStart, today) + 1;
            if (days <= 7) {
                return 7;
            }
            if (days <= 30) {
                return 30;
            }
            return 90;
        }
        return range.getDays() > 0 ? range.getDays() : 30;
    }

    private String resolveStatusLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "未知状态";
        }
        try {
            int code = Integer.parseInt(raw.trim());
            return statusLabel(code);
        } catch (NumberFormatException ex) {
            return raw;
        }
    }

    private String statusLabel(int status) {
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
