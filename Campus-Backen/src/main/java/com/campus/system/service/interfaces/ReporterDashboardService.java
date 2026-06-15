package com.campus.system.service.interfaces;

import com.campus.system.vo.ReporterDashboardVO;

// 报修人首页看板
public interface ReporterDashboardService {

    ReporterDashboardVO dashboard(Integer rangeDays);
}
