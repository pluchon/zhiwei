package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 工单状态流转日志响应数据
@Data
public class RepairOrderLogVO {

    // 流转日志主键
    private Long logId;

    // 所属工单主键
    private Long orderId;

    // 操作人用户主键
    private Long operatorId;

    // 流程动作编码
    private Integer action;

    // 变更前工单状态
    private Integer fromStatus;

    // 变更后工单状态
    private Integer toStatus;

    // 流程备注或原因
    private String remark;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
