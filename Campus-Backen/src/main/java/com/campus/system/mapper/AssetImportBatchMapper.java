package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.AssetImportBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

// 资产导入批次数据访问
@Mapper
public interface AssetImportBatchMapper extends BaseMapper<AssetImportBatch> {

    @Update("UPDATE asset_import_batch SET delete_state=1, update_time=NOW() WHERE batch_id=#{batchId} AND delete_state=0 AND pending_count > 0")
    int logicDeleteIfPending(@Param("batchId") Long batchId);
}
