package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 工单评价实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_evaluation")
public class RepairEvaluation extends BaseEntity {

    // 评价主键
    @TableId
    private Long evaluationId;

    // 所属工单主键
    private Long orderId;

    // 被评价的维修师傅用户主键
    private Long repairerId;

    // 星级评分（取值范围 1 至 5）
    private Integer star;

    // 首次评价内容
    private String content;

    // 追加评价内容
    private String followUpContent;

    // 追加评价时间
    private LocalDateTime followUpTime;
}
