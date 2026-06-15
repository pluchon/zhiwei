package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 工单图片附件实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_attachment")
public class RepairAttachment extends BaseEntity {

    // 附件主键
    @TableId
    private Long attachmentId;

    // 所属工单主键
    private Long orderId;

    // 所属维修记录主键
    private Long recordId;

    // OSS 私有对象键
    private String objectKey;

    // 上传人用户主键
    private Long uploaderId;

    // 所属草稿被逻辑删除的时间
    private java.time.LocalDateTime draftDeletedTime;

    // 计划清理到期时间
    private java.time.LocalDateTime cleanupDueTime;

    // OSS 删除状态
    private String ossDeleteStatus;

    // OSS 删除重试次数
    private Integer ossDeleteRetryCount;

    // OSS 删除最近失败原因
    private String ossDeleteFailureReason;
}
