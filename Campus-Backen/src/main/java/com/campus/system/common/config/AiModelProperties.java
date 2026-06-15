package com.campus.system.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// AI 模型与场景路由配置
@Data
@Component
@ConfigurationProperties(prefix = "campus.ai")
public class AiModelProperties {

    // 是否启用 AI 能力
    private boolean enabled = true;

    // 通用对话基座模型
    private String chatModel = "qwen-plus";

    // 多模态视觉基座模型
    private String visionModel = "qwen-vl-max";

    // 向量嵌入模型
    private String embeddingModel = "text-embedding-v3";

    // 资产识别优先模型（可配置为微调模型名）
    private String assetRecognitionModel;

    // 重复报修判断优先模型（可配置为微调模型名）
    private String duplicateRepairModel;

    // AI 调用超时秒数
    private int timeoutSeconds = 30;

    // 助手会话有效期小时数
    private int sessionTtlHours = 8;

    // Embedding 语义匹配最低相似度阈值
    private double embeddingSimilarityThreshold = 0.72;

    // Embedding 向量 Redis 缓存小时数
    private int embeddingCacheTtlHours = 24;

    public String resolveAssetRecognitionModel() {
        if (assetRecognitionModel != null && !assetRecognitionModel.isBlank()) {
            return assetRecognitionModel;
        }
        return visionModel;
    }

    public String resolveDuplicateRepairModel() {
        if (duplicateRepairModel != null && !duplicateRepairModel.isBlank()) {
            return duplicateRepairModel;
        }
        return chatModel;
    }
}
