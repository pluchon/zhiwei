package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 工单评价响应数据
@Data
public class RepairEvaluationVO {

    // 评价主键
    private Long evaluationId;

    // 所属工单主键
    private Long orderId;

    // 被评价的维修师傅用户主键
    private Long repairerId;

    // 星级评分，范围为 1 到 5
    private Integer star;

    // 首次评价内容
    private String content;

    // 追加评价内容
    private String followUpContent;

    // 追加评价时间
    private LocalDateTime followUpTime;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
