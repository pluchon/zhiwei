package com.campus.system.service.ai;

import com.campus.system.common.enums.AiResultStatus;
import com.campus.system.common.enums.AiSceneType;
import com.campus.system.entity.AiAuditLog;
import com.campus.system.mapper.AiAuditLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// AI 场景级审计服务，不记录 prompt 明文
@Service
public class AiAuditService {

    @Autowired
    private AiAuditLogMapper auditLogs;

    @Async
    public void recordSuccess(AiSceneType scene, Long operatorId, String targetType, Long targetId) {
        insert(scene, operatorId, targetType, targetId, AiResultStatus.SUCCESS.getCode(), null);
    }

    @Async
    public void recordDegraded(AiSceneType scene, Long operatorId, String targetType, Long targetId, String reason) {
        insert(scene, operatorId, targetType, targetId, AiResultStatus.DEGRADED.getCode(), reason);
    }

    @Async
    public void recordFailure(AiSceneType scene, Long operatorId, String targetType, Long targetId, String reason) {
        insert(scene, operatorId, targetType, targetId, AiResultStatus.FAILED.getCode(), reason);
    }

    private void insert(AiSceneType scene, Long operatorId, String targetType, Long targetId, String resultStatus, String failureReason) {
        AiAuditLog log = new AiAuditLog();
        log.setOperatorId(operatorId);
        log.setSceneType(scene.getCode());
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setResultStatus(resultStatus);
        log.setFailureReason(failureReason);
        auditLogs.insert(log);
    }
}
