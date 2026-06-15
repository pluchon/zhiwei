package com.campus.system.dto;

import lombok.Data;

// 报修工单创建与编辑请求参数
@Data
public class RepairOrderEditDTO {

    // 客户端幂等请求编号
    private String requestId;

    // 报修类型（NORMAL普通报修 ASSET资产报修）
    private String repairType;

    // 关联资产主键，资产报修时必填
    private Long assetId;

    // 工单标题
    private String title;

    // 故障描述
    private String description;

    // 故障类型主键
    private Long categoryId;

    // 校区主键
    private Long campusId;

    // 楼栋主键，可为空
    private Long buildingId;

    // 所在楼层，可为空
    private String floor;

    // 教室或房间，可为空
    private String room;

    // 具体位置说明
    private String locationDetail;

    // 报修联系电话
    private String contactPhone;

    // 编辑时携带的乐观锁版本号
    private Integer version;
}
