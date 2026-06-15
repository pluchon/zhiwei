package com.campus.system.service.profile;

import com.campus.system.common.enums.AiSceneType;
import com.campus.system.service.ai.AiCallResult;
import com.campus.system.service.ai.AiClientGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 头像 AI 审核
@Component
public class AvatarModerationService {

    private static final Logger log = LoggerFactory.getLogger(AvatarModerationService.class);

    private static final String PROMPT =
            """
            你是校园管理系统头像审核员。判断图片是否适合作为个人头像。
            仅返回 JSON：{"approved": true/false, "reason": "20字以内说明"}
            通过标准：清晰的人物肖像或半身照，适合校园身份展示。
            驳回标准：色情暴力、政治敏感、二维码广告、纯文字截图、表情包、风景动物、主体不明、空白或严重模糊。
            """;

    @Autowired
    private AiClientGateway aiGateway;

    @Autowired
    private ObjectMapper objectMapper;

    public AvatarReviewResult review(String imageUrl, Long userId) {
        if (!aiGateway.isAvailable()) {
            return AvatarReviewResult.rejected("头像审核服务暂不可用，请稍后重试");
        }
        AiCallResult result = aiGateway.vision(AiSceneType.AVATAR_REVIEW, userId, userId, "USER_AVATAR", imageUrl, PROMPT);
        if (!result.isSuccess() || result.getContent() == null || result.getContent().isBlank()) {
            return AvatarReviewResult.rejected("头像审核失败，请更换图片后重试");
        }
        try {
            String json = extractJson(result.getContent());
            JsonNode node = objectMapper.readTree(json);
            boolean approved = node.path("approved").asBoolean(false);
            String reason = node.path("reason").asText("不符合头像规范");
            if (approved) {
                return AvatarReviewResult.approved();
            }
            return AvatarReviewResult.rejected(reason);
        } catch (Exception ex) {
            log.warn("解析头像审核结果失败 userId={}", userId, ex);
            return AvatarReviewResult.rejected("头像审核结果异常，请更换图片后重试");
        }
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }
}
