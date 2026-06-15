package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.AiAssistantMessage;
import org.apache.ibatis.annotations.Mapper;

// AI 助手消息 Mapper
@Mapper
public interface AiAssistantMessageMapper extends BaseMapper<AiAssistantMessage> {
}
