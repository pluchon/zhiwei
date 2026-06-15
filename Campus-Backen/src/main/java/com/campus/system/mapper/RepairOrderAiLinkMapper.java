package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.RepairOrderAiLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

// 工单 AI 关联 Mapper
@Mapper
public interface RepairOrderAiLinkMapper extends BaseMapper<RepairOrderAiLink> {

    // 逻辑删除指定关联
    @Update("UPDATE repair_order_ai_link SET delete_state = 1, update_time = NOW(), operator_id = #{operatorId} "
            + "WHERE link_id = #{linkId} AND delete_state = 0")
    int logicDeleteLink(@Param("linkId") Long linkId, @Param("operatorId") Long operatorId);

    // 确认关联
    @Update("UPDATE repair_order_ai_link SET confirmed = 1, update_time = NOW(), operator_id = #{operatorId} "
            + "WHERE link_id = #{linkId} AND delete_state = 0 AND confirmed = 0")
    int confirmLink(@Param("linkId") Long linkId, @Param("operatorId") Long operatorId);
}
