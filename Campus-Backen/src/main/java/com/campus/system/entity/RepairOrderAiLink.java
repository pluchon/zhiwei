package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 工单 AI 关联推荐实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_order_ai_link")
public class RepairOrderAiLink extends BaseEntity {

    // 关联记录主键
    @TableId
    private Long linkId;

    // 当前工单主键
    private Long sourceOrderId;

    // 关联历史工单主键
    private Long targetOrderId;

    // 关联类型
    private String linkType;

    // AI 推荐理由
    private String aiReason;

    // 管理员是否已确认
    private Integer confirmed;

    // 确认或解除操作人主键
    private Long operatorId;
}
