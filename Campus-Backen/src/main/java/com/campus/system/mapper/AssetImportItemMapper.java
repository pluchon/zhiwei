package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.AssetImportItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

// 待审核资产卡片数据访问
@Mapper
public interface AssetImportItemMapper extends BaseMapper<AssetImportItem> {

    @Update("UPDATE asset_import_item SET status=#{toStatus}, confirmed_asset_id=#{assetId}, failure_reason=NULL, update_time=NOW() "
            + "WHERE item_id=#{itemId} AND status='PENDING' AND delete_state=0")
    int confirmIfPending(@Param("itemId") Long itemId, @Param("toStatus") String toStatus, @Param("assetId") Long assetId);

    @Update("UPDATE asset_import_item SET status='IGNORED', failure_reason=NULL, update_time=NOW() "
            + "WHERE item_id=#{itemId} AND status='PENDING' AND delete_state=0")
    int ignoreIfPending(@Param("itemId") Long itemId);

    @Update("UPDATE asset_import_item SET failure_reason=#{reason}, update_time=NOW() "
            + "WHERE item_id=#{itemId} AND status='PENDING' AND delete_state=0")
    int markFailure(@Param("itemId") Long itemId, @Param("reason") String reason);

    @Update("UPDATE asset_import_item SET delete_state=1, update_time=NOW() "
            + "WHERE batch_id=#{batchId} AND status='PENDING' AND delete_state=0")
    int logicDeletePendingByBatch(@Param("batchId") Long batchId);
}
