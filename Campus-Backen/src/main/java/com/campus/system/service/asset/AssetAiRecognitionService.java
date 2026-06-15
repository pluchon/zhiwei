package com.campus.system.service.asset;

import com.campus.system.common.enums.AiSceneType;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.dto.AiRecognizedAssetFields;
import com.campus.system.service.ai.AiCallResult;
import com.campus.system.service.ai.AiClientGateway;
import com.campus.system.service.ai.AiModelRouteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 资产 AI 识别服务（Excel 增强与图片识别）
@Component
public class AssetAiRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(AssetAiRecognitionService.class);

    private static final String SYSTEM_PROMPT =
    """
    你是校园资产台账识别助手。请从输入内容中提取资产字段，仅返回 JSON，不要输出其他文字。
    JSON 字段：assetName, categoryText, purchaseDate, enabledDate, assetDescription。
    日期格式 yyyy-MM-dd，无法识别则留空字符串。不要填写校区、楼栋或位置信息。
    """;

    private static final String VISION_PROMPT =
    """
    请识别图片中的资产信息，返回 JSON：assetName, categoryText, purchaseDate, enabledDate, assetDescription。
    日期格式 yyyy-MM-dd，无法识别则留空字符串。不要填写校区、楼栋或位置信息。
    """;

    @Autowired
    private AiClientGateway aiGateway;

    @Autowired
    private AiModelRouteService modelRoute;

    @Autowired
    private ObjectMapper objectMapper;

    // 对 Excel 行文本做 AI 字段补全
    public AiRecognizedAssetFields enhanceExcelRow(String rowText) {
        if (rowText == null || rowText.isBlank()) {
            return null;
        }
        AiCallResult result = aiGateway.chat(AiSceneType.ASSET_RECOGNITION, SecurityUtils.current().userId(), null,
                "ASSET_IMPORT", modelRoute.assetRecognitionModel(), SYSTEM_PROMPT, "请识别以下 Excel 行内容：\n" + rowText);
        return parseFields(result);
    }

    // 对图片 URL 做 AI 识别
    public AiRecognizedAssetFields recognizeImage(String imageUrl, Long itemId) {
        AiCallResult result = aiGateway.vision(AiSceneType.ASSET_RECOGNITION, SecurityUtils.current().userId(), itemId, "ASSET_IMPORT_ITEM", imageUrl, VISION_PROMPT);
        return parseFields(result);
    }

    private AiRecognizedAssetFields parseFields(AiCallResult result) {
        if (result == null || !result.isSuccess() || result.getContent() == null || result.getContent().isBlank()) {
            return null;
        }
        try {
            String json = extractJson(result.getContent());
            return objectMapper.readValue(json, AiRecognizedAssetFields.class);
        } catch (Exception ex) {
            log.warn("解析 AI 资产识别结果失败", ex);
            return null;
        }
    }

    public LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            return null;
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
