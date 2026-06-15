package com.campus.system.service.ai;

import com.campus.system.common.config.AiModelProperties;
import com.campus.system.common.enums.AiResultStatus;
import com.campus.system.common.enums.AiSceneType;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import com.campus.system.common.config.AiModelProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

// 统一封装 DashScope 模型调用与降级
@Component
public class AiClientGateway {

    private static final Logger log = LoggerFactory.getLogger(AiClientGateway.class);

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private AiModelProperties properties;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Autowired
    private AiAuditService auditService;

    // 文本对话，优先使用指定模型，失败时降级到基座模型
    public AiCallResult chat(AiSceneType scene, Long operatorId, Long targetId, String targetType, String modelName, String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            return AiCallResult.degraded("AI 未启用或未配置密钥");
        }
        String primaryModel = modelName != null && !modelName.isBlank() ? modelName : properties.getChatModel();
        try {
            String content = invokeChat(primaryModel, systemPrompt, userPrompt);
            auditService.recordSuccess(scene, operatorId, targetType, targetId);
            return AiCallResult.success(content);
        } catch (Exception primaryEx) {
            log.warn("AI 主模型调用失败 scene={} model={}", scene.getCode(), primaryModel, primaryEx);
            if (!primaryModel.equals(properties.getChatModel())) {
                try {
                    String content = invokeChat(properties.getChatModel(), systemPrompt, userPrompt);
                    auditService.recordDegraded(scene, operatorId, targetType, targetId, "主模型不可用，已降级基座模型");
                    return AiCallResult.degradedSuccess(content, "已降级到基座模型");
                } catch (Exception fallbackEx) {
                    log.warn("AI 基座模型调用失败 scene={}", scene.getCode(), fallbackEx);
                }
            }
            auditService.recordFailure(scene, operatorId, targetType, targetId, truncate(primaryEx.getMessage()));
            return AiCallResult.degraded("AI 调用失败");
        }
    }

    // 多模态图片识别
    public AiCallResult vision(AiSceneType scene, Long operatorId, Long targetId, String targetType,
            String imageUrl, String userPrompt) {
        if (!isAvailable()) {
            return AiCallResult.degraded("AI 未启用或未配置密钥");
        }
        String primaryModel = properties.resolveAssetRecognitionModel();
        try {
            String content = invokeVision(primaryModel, imageUrl, userPrompt);
            auditService.recordSuccess(scene, operatorId, targetType, targetId);
            return AiCallResult.success(content);
        } catch (Exception primaryEx) {
            log.warn("AI 视觉模型调用失败 scene={} model={}", scene.getCode(), primaryModel, primaryEx);
            if (!primaryModel.equals(properties.getVisionModel())) {
                try {
                    String content = invokeVision(properties.getVisionModel(), imageUrl, userPrompt);
                    auditService.recordDegraded(scene, operatorId, targetType, targetId, "视觉微调模型不可用，已降级基座模型");
                    return AiCallResult.degradedSuccess(content, "已降级到基座视觉模型");
                } catch (Exception fallbackEx) {
                    log.warn("AI 基座视觉模型调用失败 scene={}", scene.getCode(), fallbackEx);
                }
            }
            auditService.recordFailure(scene, operatorId, targetType, targetId, truncate(primaryEx.getMessage()));
            return AiCallResult.degraded("AI 视觉识别失败");
        }
    }

    public boolean isAvailable() {
        return properties.isEnabled() && apiKey != null && !apiKey.isBlank();
    }

    private String invokeChat(String model, String systemPrompt, String userPrompt) throws Exception {
        StringBuilder promptText = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            promptText.append(systemPrompt).append("\n\n");
        }
        promptText.append(userPrompt);
        Prompt prompt = new Prompt(List.of(new UserMessage(promptText.toString())), ChatOptions.builder().model(model).build());
        return callWithTimeout(prompt);
    }

    private String invokeVision(String model, String imageUrl, String userPrompt) throws Exception {
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, java.net.URI.create(imageUrl));
        UserMessage message = UserMessage.builder().text(userPrompt).media(media).build();
        Prompt prompt = new Prompt(List.of(message), ChatOptions.builder().model(model).build());
        return callWithTimeout(prompt);
    }

    private String callWithTimeout(Prompt prompt) throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            ChatResponse response = chatModel.call(prompt);
            response.getResult();
            response.getResult();
            return response.getResult().getOutput().getText();
        });
        try {
            return future.get(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new IllegalStateException("AI 调用超时");
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
