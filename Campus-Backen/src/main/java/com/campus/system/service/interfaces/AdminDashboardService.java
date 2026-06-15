package com.campus.system.service.interfaces;

import com.campus.system.vo.RepairDashboardVO;

/**
 * 管理员报修看板业务接口。
 */
public interface AdminDashboardService {

  RepairDashboardVO dashboard(Integer rangeDays);
}
