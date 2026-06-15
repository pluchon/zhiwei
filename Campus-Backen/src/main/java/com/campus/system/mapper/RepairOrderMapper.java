package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

// 并发控制核心逻辑，只写在mapper中
@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {

  @Update("UPDATE repair_order SET status=#{to}, current_repairer_id=#{repairerId}, version=version+1, update_time=NOW() "
          + "WHERE order_id=#{id} AND status=#{from} AND version=#{version} AND delete_state=0")
  int transition(@Param("id") Long id, @Param("from") int from, @Param("to") int to, @Param("version") int version, @Param("repairerId") Long repairerId);

  @Update("UPDATE repair_order SET status=#{to}, current_repairer_id=#{repairerId}, version=version+1, update_time=NOW() "
          + "WHERE order_id=#{id} AND status IN (1,2) AND version=#{version} AND delete_state=0")
  int adminDispatch(@Param("id") Long id, @Param("to") int to, @Param("version") int version, @Param("repairerId") Long repairerId);

  @Update("UPDATE repair_order SET status=0, current_repairer_id=NULL, version=version+1, update_time=NOW() "
          + "WHERE order_id=#{id} AND status IN (1,2) AND reporter_id=#{reporterId} AND version=#{version} AND delete_state=0")
  int withdrawToDraft(@Param("id") Long id, @Param("reporterId") Long reporterId, @Param("version") int version);

  @Update("UPDATE repair_order SET status=0, version=version+1, update_time=NOW() "
          + "WHERE order_id=#{id} AND status=8 AND reporter_id=#{reporterId} AND version=#{version} AND delete_state=0")
  int rejectedToDraft(@Param("id") Long id, @Param("reporterId") Long reporterId, @Param("version") int version);

  @Update("UPDATE repair_order SET unresolved_count=unresolved_count+1, "
          + "status=CASE WHEN unresolved_count+1>=5 THEN 6 ELSE 4 END, version=version+1, update_time=NOW() "
          + "WHERE order_id=#{id} AND status=5 AND reporter_id=#{reporterId} AND version=#{version} AND delete_state=0")
  int feedbackUnresolved(@Param("id") Long id, @Param("reporterId") Long reporterId, @Param("version") int version);

  @Update("UPDATE repair_order SET status=7, completion_time=NOW(), version=version+1, update_time=NOW() "
          + "WHERE order_id=#{id} AND status=5 AND version=#{version} AND delete_state=0")
  int confirmComplete(@Param("id") Long id, @Param("version") int version);

  @Update("UPDATE repair_order SET status=7, completion_time=NOW(), auto_completed_time=NOW(), "
          + "version=version+1, update_time=NOW() WHERE order_id=#{id} AND status=5 AND delete_state=0")
  int autoComplete(@Param("id") Long id);

}