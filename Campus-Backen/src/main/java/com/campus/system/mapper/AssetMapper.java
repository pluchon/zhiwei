package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.campus.system.entity.Asset;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// 资产台账数据访问，含并发条件更新
@Mapper
public interface AssetMapper extends BaseMapper<Asset> {

    @Update("UPDATE asset SET active_order_id=#{orderId}, version=version+1, update_time=NOW() "
            + "WHERE asset_id=#{assetId} AND delete_state=0 AND status='IN_USE' AND active_order_id IS NULL AND version=#{version}")
    int claimActiveOrder(@Param("assetId") Long assetId, @Param("orderId") Long orderId, @Param("version") int version);

    @Update("UPDATE asset SET active_order_id=NULL, version=version+1, update_time=NOW() "
            + "WHERE asset_id=#{assetId} AND active_order_id=#{orderId} AND delete_state=0")
    void releaseActiveOrder(@Param("assetId") Long assetId, @Param("orderId") Long orderId);

    @Update("UPDATE asset SET status='UNDER_REPAIR', version=version+1, update_time=NOW() "
            + "WHERE asset_id=#{assetId} AND active_order_id=#{orderId} AND status='IN_USE' AND delete_state=0")
    int markUnderRepair(@Param("assetId") Long assetId, @Param("orderId") Long orderId);

    @Update("UPDATE asset SET status=#{status}, version=version+1, update_time=NOW() "
            + "WHERE asset_id=#{assetId} AND delete_state=0 AND active_order_id IS NULL AND version=#{version}")
    int updateStatusIfNoActiveOrder(@Param("assetId") Long assetId, @Param("status") String status, @Param("version") int version);

    @Update("UPDATE asset SET status='IN_USE', version=version+1, update_time=NOW() "
            + "WHERE asset_id=#{assetId} AND active_order_id IS NULL AND status='UNDER_REPAIR' AND delete_state=0")
    int restoreInUseIfNoActiveOrder(@Param("assetId") Long assetId);

    @Update("UPDATE asset SET delete_state=1, version=version+1, update_time=NOW() "
            + "WHERE asset_id=#{assetId} AND delete_state=0 AND active_order_id IS NULL AND version=#{version}")
    int logicDeleteIfNoActiveOrder(@Param("assetId") Long assetId, @Param("version") int version);

    @Update("UPDATE asset SET delete_state=0, status='IN_USE', version=version+1, update_time=NOW() "
            + "WHERE asset_id=#{assetId} AND delete_state=1")
    int restoreDeleted(@Param("assetId") Long assetId);

    @Select("SELECT asset_no FROM asset WHERE asset_no LIKE CONCAT(#{prefix}, '%') ORDER BY asset_no DESC LIMIT 1")
    String findLatestAssetNoIncludingDeleted(@Param("prefix") String prefix);

    @Select("SELECT * FROM asset WHERE asset_id = #{id}")
    Asset selectByIdIncludeDeleted(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM (SELECT asset_id FROM asset ${ew.customSqlSegment}) tmp")
    Long selectCountIncludeDeleted(@Param(Constants.WRAPPER) Wrapper<Asset> wrapper);

    @Select("SELECT * FROM asset ${ew.customSqlSegment}")
    List<Asset> selectListIncludeDeleted(@Param(Constants.WRAPPER) Wrapper<Asset> wrapper);
}
