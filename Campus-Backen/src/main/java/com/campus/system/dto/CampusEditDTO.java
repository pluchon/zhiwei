package com.campus.system.dto;

import lombok.Data;

// 校区创建与编辑请求参数
@Data
public class CampusEditDTO {

  // 校区名称
  private String campusName;

  // 校区说明
  private String description;
}
