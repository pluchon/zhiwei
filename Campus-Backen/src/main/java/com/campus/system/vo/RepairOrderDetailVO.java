package com.campus.system.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

// 工单详情响应数据
@Data
@AllArgsConstructor
public class RepairOrderDetailVO {

    // 工单主体信息
    private RepairOrderVO order;

    // 工单所属故障类型
    private RepairCategoryVO category;

    // 工单图片附件列表
    private List<RepairAttachmentVO> attachments;

    // 维修结果记录列表
    private List<RepairRecordVO> records;

    // 工单评论列表
    private List<RepairCommentVO> comments;

    // 工单状态流转日志列表
    private List<RepairOrderLogVO> logs;

    // 报修人服务评价（已评价时返回）
    private RepairEvaluationVO evaluation;
}
