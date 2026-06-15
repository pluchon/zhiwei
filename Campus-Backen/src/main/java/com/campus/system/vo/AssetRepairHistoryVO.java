package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 资产维修历史响应数据
@Data
public class AssetRepairHistoryVO {

    // 工单主键
    private Long orderId;

    // 工单编号
    private String orderNo;

    // 故障类型名称
    private String categoryName;

    // 故障描述
    private String description;

    // 维修师傅账号
    private String repairerUserNo;

    // 维修师傅姓名
    private String repairerRealName;

    // 维修结果描述
    private String repairResult;

    // 确认完成时间
    private LocalDateTime completionTime;
}
