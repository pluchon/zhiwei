package com.campus.system.dto;

import java.util.List;
import lombok.Data;

// 批量标记通知已读请求参数
@Data
public class NotificationBatchReadDTO {

  // 通知主键列表
  private List<Long> notificationIds;
}
