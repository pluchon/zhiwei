package com.campus.system.dto;

import lombok.Data;

// 管理员手动派单请求参数
@Data
public class AdminDispatchDTO {

  // 目标维修师傅用户主键
  private Long repairerId;

  // 派单说明
  private String dispatchNote;

  // 能力不匹配派单原因，可为空
  private String capabilityMismatchReason;

  // 工单乐观锁版本号
  private Integer version;
}
