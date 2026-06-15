package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.StatisticsQueryDTO;
import com.campus.system.service.interfaces.ManagementStatisticsExportService;
import com.campus.system.service.interfaces.ManagementStatisticsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 管理统计接口
@RestController
public class ManagementStatisticsController {

    @Autowired
    private ManagementStatisticsService statisticsService;

    @Autowired
    private ManagementStatisticsExportService exportService;

    /**
     * 管理员查询管理统计汇总
     */
    @GetMapping("/admin/statistics/management")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> overview(StatisticsQueryDTO query) {
        return ApiResponse.ok(statisticsService.overview(query));
    }

    /**
     * 管理员导出管理统计 Excel
     */
    @GetMapping("/admin/statistics/management/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void export(StatisticsQueryDTO query, HttpServletResponse response) {
        exportService.export(query, response);
    }

    /**
     * 维修师傅查询个人工作统计
     */
    @GetMapping("/repair/repairer/statistics")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> personal(StatisticsQueryDTO query) {
        return ApiResponse.ok(statisticsService.personal(query));
    }
}
