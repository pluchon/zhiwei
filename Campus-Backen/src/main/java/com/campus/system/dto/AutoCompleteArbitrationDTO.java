package com.campus.system.dto;

import lombok.Data;

// 自动完成后申请仲裁请求参数
@Data
public class AutoCompleteArbitrationDTO {

  // 仲裁申请说明
  private String reason;

  // 工单乐观锁版本号
  private Integer version;
}
