package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 维修记录响应数据
@Data
public class RepairRecordVO {

    // 维修记录主键
    private Long recordId;

    // 所属工单主键
    private Long orderId;

    // 提交维修结果的维修师傅用户主键
    private Long repairerId;

    // 维修结果描述
    private String resultDescription;

    // 当前工单第几次维修尝试
    private Integer attemptNo;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
