package com.campus.system.service.interfaces;

import com.campus.system.dto.StatisticsQueryDTO;
import com.campus.system.vo.ManagementStatisticsVO;
import com.campus.system.vo.RepairerWorkStatVO;

// 管理统计业务接口
public interface ManagementStatisticsService {

    ManagementStatisticsVO overview(StatisticsQueryDTO query);

    RepairerWorkStatVO personal(StatisticsQueryDTO query);
}
