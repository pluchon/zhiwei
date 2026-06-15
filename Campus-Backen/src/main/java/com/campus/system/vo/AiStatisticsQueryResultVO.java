package com.campus.system.vo;

import com.campus.system.vo.ManagementStatisticsVO;
import com.campus.system.vo.RepairerWorkStatVO;
import java.util.List;
import lombok.Data;

// 自然语言查统计结果
@Data
public class AiStatisticsQueryResultVO {

    // 结果类型（ADMIN_OVERVIEW / REPAIRER_PERSONAL）
    private String resultType;

    // 管理员全局统计
    private ManagementStatisticsVO overview;

    // 维修师傅个人统计
    private RepairerWorkStatVO personal;

    // 结果说明
    private String summary;

    // AI 动态图表（按用户意图生成，数据来自真实统计）
    private List<AiDynamicChartVO> charts;
}
