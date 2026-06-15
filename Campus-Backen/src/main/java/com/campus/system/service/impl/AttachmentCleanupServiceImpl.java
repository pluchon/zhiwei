package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.entity.RepairAttachment;
import com.campus.system.entity.SysOperationLog;
import com.campus.system.mapper.RepairAttachmentMapper;
import com.campus.system.mapper.SysOperationLogMapper;
import com.campus.system.service.interfaces.AttachmentCleanupService;
import com.campus.system.service.interfaces.OssService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 冗余附件清理实现
@Service
public class AttachmentCleanupServiceImpl implements AttachmentCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentCleanupServiceImpl.class);

    @Autowired
    private RepairAttachmentMapper attachments;

    @Autowired
    private OssService oss;

    @Autowired
    private SysOperationLogMapper operationLogs;

    @Autowired
    private BusinessClock clock;

    @Override
    public void processCleanup() {
        LocalDateTime now = clock.now();
        List<RepairAttachment> candidates = attachments.selectList(Wrappers.<RepairAttachment>lambdaQuery().eq(RepairAttachment::getDeleteState, 0)
                .in(RepairAttachment::getOssDeleteStatus, "NONE", "FAILED")
                .and(wrapper -> wrapper
                        .isNull(RepairAttachment::getOrderId).lt(RepairAttachment::getCreateTime, now.minusHours(24))
                        .or().isNotNull(RepairAttachment::getCleanupDueTime).le(RepairAttachment::getCleanupDueTime, now)));
        for (RepairAttachment attachment : candidates) {
            try {
                cleanupSingle(attachment.getAttachmentId());
            } catch (Exception ex) {
                log.warn("附件清理单条失败，attachmentId={}", attachment.getAttachmentId());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected void cleanupSingle(Long attachmentId) {
        RepairAttachment attachment = attachments.selectById(attachmentId);
        if (attachment == null || attachment.getDeleteState() != 0) {
            return;
        }
        if (!canDelete(attachment)) {
            return;
        }
        try {
            oss.delete(attachment.getObjectKey());
            attachment.setOssDeleteStatus("SUCCESS");
            attachment.setOssDeleteFailureReason(null);
            attachments.updateById(attachment);
            // delete_state 是 MyBatis-Plus 逻辑删除字段，updateById 不会将它作为普通字段更新。
            attachments.deleteById(attachmentId);
            writeLog("ATTACHMENT_CLEANUP_SUCCESS", attachment.getObjectKey(), "清理成功");
        } catch (Exception ex) {
            attachment.setOssDeleteStatus("FAILED");
            attachment.setOssDeleteRetryCount((attachment.getOssDeleteRetryCount() == null ? 0 : attachment.getOssDeleteRetryCount()) + 1);
            attachment.setOssDeleteFailureReason(ex.getMessage());
            attachments.updateById(attachment);
            writeLog("ATTACHMENT_CLEANUP_FAILED", attachment.getObjectKey(), ex.getMessage());
        }
    }

    private boolean canDelete(RepairAttachment attachment) {
        if (attachment.getOrderId() == null) {
            return true;
        }
        if (attachment.getCleanupDueTime() != null && !attachment.getCleanupDueTime().isAfter(clock.now())) {
            return attachments.selectCount(Wrappers.<RepairAttachment>lambdaQuery().eq(RepairAttachment::getObjectKey, attachment.getObjectKey()).eq(RepairAttachment::getDeleteState, 0)) == 1;
        }
        return false;
    }

    private void writeLog(String type, String objectKey, String description) {
        SysOperationLog logEntry = new SysOperationLog();
        logEntry.setOperationType(type);
        logEntry.setTargetType("ATTACHMENT");
        logEntry.setDescription(objectKey + "：" + description);
        operationLogs.insert(logEntry);
    }
}
