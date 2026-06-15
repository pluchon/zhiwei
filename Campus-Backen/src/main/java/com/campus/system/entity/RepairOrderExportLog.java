package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 工单导出日志实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_order_export_log")
public class RepairOrderExportLog extends BaseEntity {

    // 导出日志主键
    @TableId
    private Long exportLogId;

    // 操作管理员主键
    private Long operatorId;

    // 筛选条件快照
    private String filterSnapshot;

    // 导出数量
    private Integer exportCount;

    // 执行结果
    private String resultStatus;

    // 导出文件名
    private String fileName;

    // 失败原因
    private String failureReason;
}
