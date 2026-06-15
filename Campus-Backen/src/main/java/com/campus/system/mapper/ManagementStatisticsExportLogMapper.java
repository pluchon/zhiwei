package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.ManagementStatisticsExportLog;
import org.apache.ibatis.annotations.Mapper;

// 管理统计导出日志数据访问
@Mapper
public interface ManagementStatisticsExportLogMapper extends BaseMapper<ManagementStatisticsExportLog> {
}
