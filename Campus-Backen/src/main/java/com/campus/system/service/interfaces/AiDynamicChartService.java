package com.campus.system.service.interfaces;

import com.campus.system.vo.AiDynamicChartVO;
import java.util.List;

// AI 助手动态图表业务接口
public interface AiDynamicChartService {

    List<AiDynamicChartVO> buildCharts(String role, String rangeType, String chartTypeCode, String dimensionCode, String message);
}
