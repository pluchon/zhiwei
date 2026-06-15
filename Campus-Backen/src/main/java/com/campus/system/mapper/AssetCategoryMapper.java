package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.AssetCategory;
import org.apache.ibatis.annotations.Mapper;

// 资产分类数据访问
@Mapper
public interface AssetCategoryMapper extends BaseMapper<AssetCategory> {
}
