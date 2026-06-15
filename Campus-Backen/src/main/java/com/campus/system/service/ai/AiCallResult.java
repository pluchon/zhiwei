package com.campus.system.service.ai;

import com.campus.system.common.enums.AiResultStatus;
import lombok.Getter;

// AI 调用结果封装
@Getter
public class AiCallResult {

    private final boolean success;
    private final boolean degraded;
    private final String content;
    private final String message;

    private AiCallResult(boolean success, boolean degraded, String content, String message) {
        this.success = success;
        this.degraded = degraded;
        this.content = content;
        this.message = message;
    }

    public static AiCallResult success(String content) {
        return new AiCallResult(true, false, content, null);
    }

    public static AiCallResult degradedSuccess(String content, String message) {
        return new AiCallResult(true, true, content, message);
    }

    public static AiCallResult degraded(String message) {
        return new AiCallResult(false, true, null, message);
    }

    public AiResultStatus resultStatus() {
        if (success && !degraded) {
            return AiResultStatus.SUCCESS;
        }
        if (success) {
            return AiResultStatus.DEGRADED;
        }
        return AiResultStatus.FAILED;
    }
}
