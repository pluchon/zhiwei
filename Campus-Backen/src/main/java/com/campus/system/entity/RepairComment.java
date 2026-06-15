package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 工单评论实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_order_comment")
public class RepairComment extends BaseEntity {

    // 评论主键
    @TableId
    private Long commentId;

    // 所属工单主键
    private Long orderId;

    // 评论作者用户主键
    private Long authorId;

    // 评论类型（0用户评论 1系统流程评论）
    private Integer commentType;

    // 评论内容
    private String content;

    // 置顶状态（0普通 1置顶）
    private Integer isPinned;

    // 撤回状态（0正常 1已撤回）
    private Integer isWithdrawn;

    // 评论撤回时间
    private LocalDateTime withdrawTime;
}
