package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.RepairOrderExportLog;
import org.apache.ibatis.annotations.Mapper;

// 工单导出日志数据访问
@Mapper
public interface RepairOrderExportLogMapper extends BaseMapper<RepairOrderExportLog> {
}
