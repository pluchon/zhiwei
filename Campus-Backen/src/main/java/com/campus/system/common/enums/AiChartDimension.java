package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import java.util.Set;
import lombok.Getter;

// AI 助手动态图表统计维度（白名单）
@Getter
public enum AiChartDimension {

    FAULT_TYPE("FAULT_TYPE", "故障类型分布", true, false),
    CAMPUS("CAMPUS", "校区分布", true, false),
    BUILDING("BUILDING", "楼栋分布", true, false),
    ORDER_STATUS("ORDER_STATUS", "当前工单状态分布", true, false),
    ASSET_CATEGORY("ASSET_CATEGORY", "资产分类报修分布", true, false),
    UNFINISHED_TREND("UNFINISHED_TREND", "未完成工单趋势", true, true),
    TOP_REPAIRED_ASSETS("TOP_REPAIRED_ASSETS", "维修次数较多资产", true, false),
    REPAIR_EFFICIENCY("REPAIR_EFFICIENCY", "维修效率指标", true, false),
    REPAIRER_STATUS("REPAIRER_STATUS", "我的工单状态分布", false, false),
    REPAIRER_FAULT_TYPE("REPAIRER_FAULT_TYPE", "我的维修类型分布", false, false),
    REPAIRER_COMPLETION_TREND("REPAIRER_COMPLETION_TREND", "我的完成趋势", false, true),
    REPAIRER_WORK_SUMMARY("REPAIRER_WORK_SUMMARY", "我的工作概览", false, false);

    private static final Set<AiChartDimension> ADMIN_SET = Set.of(
            FAULT_TYPE, CAMPUS, BUILDING, ORDER_STATUS, ASSET_CATEGORY,
            UNFINISHED_TREND, TOP_REPAIRED_ASSETS, REPAIR_EFFICIENCY);

    private static final Set<AiChartDimension> REPAIRER_SET = Set.of(
            REPAIRER_STATUS, REPAIRER_FAULT_TYPE, REPAIRER_COMPLETION_TREND, REPAIRER_WORK_SUMMARY);

    private final String code;
    private final String label;
    private final boolean adminOnly;
    private final boolean trendOnly;

    AiChartDimension(String code, String label, boolean adminOnly, boolean trendOnly) {
        this.code = code;
        this.label = label;
        this.adminOnly = adminOnly;
        this.trendOnly = trendOnly;
    }

    public static AiChartDimension of(String code) {
        if (code == null || code.isBlank()) {
            throw BusinessException.badRequest("未指定统计维度");
        }
        String normalized = code.trim().toUpperCase();
        for (AiChartDimension value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        throw BusinessException.badRequest("不支持的统计维度");
    }

    public static AiChartDimension resolveForRole(String role, String code, String message) {
        AiChartDimension dimension;
        if (code == null || code.isBlank()) {
            dimension = defaultForRole(role, message);
        } else {
            try {
                dimension = of(code);
            } catch (BusinessException ex) {
                dimension = fromMessage(role, message);
            }
        }
        assertRoleAllowed(role, dimension);
        return dimension;
    }

    public static AiChartDimension fromMessage(String role, String message) {
        if (message == null || message.isBlank()) {
            return defaultForRole(role, message);
        }
        if ("ADMIN".equals(role)) {
            if (message.contains("楼栋")) {
                return BUILDING;
            }
            if (message.contains("校区")) {
                return CAMPUS;
            }
            if (message.contains("资产分类") || message.contains("分类报修")) {
                return ASSET_CATEGORY;
            }
            if (message.contains("未完成") || message.contains("未完结")) {
                return UNFINISHED_TREND;
            }
            if (message.contains("效率") || message.contains("时长")) {
                return REPAIR_EFFICIENCY;
            }
            if (message.contains("资产") && (message.contains("TOP") || message.contains("最多") || message.contains("排名"))) {
                return TOP_REPAIRED_ASSETS;
            }
            if (message.contains("状态")) {
                return ORDER_STATUS;
            }
            if (message.contains("故障") || message.contains("类型")) {
                return FAULT_TYPE;
            }
            return FAULT_TYPE;
        }
        if (message.contains("完成") && (message.contains("趋势") || message.contains("走势"))) {
            return REPAIRER_COMPLETION_TREND;
        }
        if (message.contains("类型") || message.contains("故障")) {
            return REPAIRER_FAULT_TYPE;
        }
        if (message.contains("概览") || message.contains("接单") || message.contains("完成率")) {
            return REPAIRER_WORK_SUMMARY;
        }
        if (message.contains("状态")) {
            return REPAIRER_STATUS;
        }
        return REPAIRER_WORK_SUMMARY;
    }

    public static void assertRoleAllowed(String role, AiChartDimension dimension) {
        if ("ADMIN".equals(role) && !ADMIN_SET.contains(dimension)) {
            throw BusinessException.forbidden("当前角色不可查看该统计维度");
        }
        if ("REPAIRER".equals(role) && !REPAIRER_SET.contains(dimension)) {
            throw BusinessException.forbidden("当前角色不可查看该统计维度");
        }
    }

    private static AiChartDimension defaultForRole(String role, String message) {
        if ("ADMIN".equals(role)) {
            return fromMessage(role, message);
        }
        return REPAIRER_WORK_SUMMARY;
    }
}
