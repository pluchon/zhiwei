package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.AiAuditLog;
import org.apache.ibatis.annotations.Mapper;

// AI 审计日志 Mapper
@Mapper
public interface AiAuditLogMapper extends BaseMapper<AiAuditLog> {
}
