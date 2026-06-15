package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 资产状态变更日志实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("asset_status_log")
public class AssetStatusLog extends BaseEntity {

    // 日志主键
    @TableId
    private Long assetStatusLogId;

    // 资产主键
    private Long assetId;

    // 变更前状态
    private String beforeStatus;

    // 变更后状态
    private String afterStatus;

    // 变更来源
    private String changeSource;

    // 关联工单主键
    private Long relatedOrderId;

    // 操作人主键
    private Long operatorId;

    // 变更原因
    private String changeReason;
}
