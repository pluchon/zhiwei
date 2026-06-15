package com.campus.system.service.interfaces;

import com.campus.system.vo.RepairerDashboardVO;

// 维修师傅首页看板
public interface RepairerDashboardService {

    RepairerDashboardVO dashboard(Integer rangeDays);
}
