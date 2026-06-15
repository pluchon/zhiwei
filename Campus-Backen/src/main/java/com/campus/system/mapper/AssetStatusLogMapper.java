package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.AssetStatusLog;
import org.apache.ibatis.annotations.Mapper;

// 资产状态日志数据访问
@Mapper
public interface AssetStatusLogMapper extends BaseMapper<AssetStatusLog> {
}
