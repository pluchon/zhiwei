package com.campus.system.vo;

import java.util.List;
import lombok.Data;

// 管理员工单重复报修详情
@Data
public class RepairOrderDuplicateDetailVO {

    // 是否疑似重复
    private Boolean suspectedDuplicate;

    // AI 判定理由
    private String duplicateReason;

    // AI 关联推荐列表
    private List<RepairOrderAiLinkVO> links;
}
