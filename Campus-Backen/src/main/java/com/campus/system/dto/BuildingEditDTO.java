package com.campus.system.dto;

import lombok.Data;

// 楼栋创建与编辑请求参数
@Data
public class BuildingEditDTO {

  // 所属校区主键
  private Long campusId;

  // 楼栋名称
  private String buildingName;

  // 楼栋说明
  private String description;
}
