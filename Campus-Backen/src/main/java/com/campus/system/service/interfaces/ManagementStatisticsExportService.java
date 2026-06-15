package com.campus.system.service.interfaces;

import com.campus.system.dto.StatisticsQueryDTO;
import jakarta.servlet.http.HttpServletResponse;

// 管理统计导出业务接口
public interface ManagementStatisticsExportService {

    void export(StatisticsQueryDTO query, HttpServletResponse response);
}
