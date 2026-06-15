package com.campus.system.service.ai;

import com.campus.system.common.config.AiModelProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// AI 模型路由服务，按场景选择微调或基座模型
@Component
public class AiModelRouteService {

    @Autowired
    private AiModelProperties properties;

    public String assetRecognitionModel() {
        return properties.resolveAssetRecognitionModel();
    }

    public String duplicateRepairModel() {
        return properties.resolveDuplicateRepairModel();
    }

    public String chatModel() {
        return properties.getChatModel();
    }
}
