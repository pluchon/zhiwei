package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 管理统计导出日志实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("management_statistics_export_log")
public class ManagementStatisticsExportLog extends BaseEntity {

    // 导出日志主键
    @TableId
    private Long exportLogId;

    // 操作管理员主键
    private Long operatorId;

    // 统计时间范围类型
    private String rangeType;

    // 导出执行结果
    private String resultStatus;

    // 导出文件名
    private String fileName;

    // 失败原因
    private String failureReason;
}
