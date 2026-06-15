package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.RepairerSuggestion;
import org.apache.ibatis.annotations.Mapper;

// 维修师傅建议数据访问
@Mapper
public interface RepairerSuggestionMapper extends BaseMapper<RepairerSuggestion> {
}
