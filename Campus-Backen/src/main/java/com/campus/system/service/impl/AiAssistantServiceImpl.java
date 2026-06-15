package com.campus.system.service.impl;

import com.campus.system.common.enums.AiChartDimension;
import com.campus.system.common.enums.AiChartType;
import com.campus.system.common.enums.AiAssistantSceneType;
import com.campus.system.common.enums.AiSceneType;
import com.campus.system.common.enums.StatisticsRangeType;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.dto.AiAssistantChatDTO;
import com.campus.system.dto.AiExportConfirmDTO;
import com.campus.system.dto.AiExportPreviewCacheDTO;
import com.campus.system.dto.AiNlExportParseResultDTO;
import com.campus.system.dto.RepairOrderQueryDTO;
import com.campus.system.dto.StatisticsQueryDTO;
import com.campus.system.service.ai.AiAssistantMessageRecord;
import com.campus.system.service.ai.AiAssistantSessionRecord;
import com.campus.system.service.ai.AiAssistantSessionStore;
import com.campus.system.service.ai.AiCallResult;
import com.campus.system.service.ai.AiClientGateway;
import com.campus.system.service.ai.AiEmbeddingGateway;
import com.campus.system.service.ai.AiModelRouteService;
import com.campus.system.service.ai.AiNlExportParseService;
import com.campus.system.service.ai.AiSemanticSearchService;
import com.campus.system.service.interfaces.AiAssistantService;
import com.campus.system.service.interfaces.AiDynamicChartService;
import com.campus.system.service.interfaces.ManagementStatisticsExportService;
import com.campus.system.service.interfaces.RepairOrderService;
import com.campus.system.dto.AssetQueryDTO;
import com.campus.system.vo.AiAssetSearchResultVO;
import com.campus.system.vo.AiOrderSearchResultVO;
import com.campus.system.vo.AiSuggestionSearchResultVO;
import com.campus.system.vo.AiAssistantHistoryMessageVO;
import com.campus.system.vo.AiAssistantMessageVO;
import com.campus.system.vo.AiAssistantSessionVO;
import com.campus.system.vo.AiExportPreviewVO;
import com.campus.system.vo.AiDynamicChartVO;
import com.campus.system.vo.AiStatisticsQueryResultVO;
import com.campus.system.service.interfaces.RepairOrderExportService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

// 统一 AI 助手业务实现（会话与消息存 Redis，7 天过期 + 逻辑删除）
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final int CONTEXT_MESSAGE_LIMIT = 12;
    private static final int CONTENT_MAX_LENGTH = 2000;

    private static final String UNIFIED_SYSTEM = """
            你是校园管理 AI 助手，帮助管理员和维修师傅查询统计、绘制图表、生成导出预览或语义搜索。
            结合对话历史理解用户意图（例如「那换成上周呢」应沿用上一轮 chartType / dimension / rangeType）。
            仅返回 JSON，格式如下：
            {
              "intent":"STATISTICS|EXPORT|ORDER_SEARCH|ASSET_SEARCH|SUGGESTION_SEARCH",
              "rangeType":"LAST_7_DAYS|LAST_30_DAYS|LAST_90_DAYS|CURRENT_YEAR",
              "chartType":"BAR|PIE|LINE",
              "dimension":"统计维度编码",
              "exportType":"ORDER|STATISTICS",
              "searchQuery":"语义搜索关键词",
              "summary":"一句话说明"
            }
            规则：
            1. 用户明确要求导出、下载、Excel 时使用 EXPORT，并填写 exportType。
            2. 搜索历史工单使用 ORDER_SEARCH（仅管理员）。
            3. 搜索资产使用 ASSET_SEARCH。
            4. 搜索建议使用 SUGGESTION_SEARCH。
            5. 用户要求查统计、画图、看分布/趋势时使用 STATISTICS，并填写 chartType 与 dimension。
            6. rangeType 根据自然语言推断，默认 LAST_30_DAYS。
            7. 管理员 dimension 可选：FAULT_TYPE、CAMPUS、BUILDING、ORDER_STATUS、ASSET_CATEGORY、UNFINISHED_TREND、TOP_REPAIRED_ASSETS、REPAIR_EFFICIENCY。
            8. 维修师傅 dimension 可选：REPAIRER_STATUS、REPAIRER_FAULT_TYPE、REPAIRER_COMPLETION_TREND、REPAIRER_WORK_SUMMARY。
            9. 趋势类维度（UNFINISHED_TREND、REPAIRER_COMPLETION_TREND）使用 LINE，占比类优先 PIE，排名类优先 BAR。
            """;

    @Autowired
    private AiAssistantSessionStore sessionStore;

    @Autowired
    private AiClientGateway aiGateway;

    @Autowired
    private AiModelRouteService modelRoute;

    @Autowired
    private AiNlExportParseService exportParseService;

    @Autowired
    private AiSemanticSearchService semanticSearchService;

    @Autowired
    private AiEmbeddingGateway embeddingGateway;

    @Autowired
    private AiDynamicChartService dynamicChartService;

    @Autowired
    private ManagementStatisticsExportService statisticsExportService;

    @Autowired
    private RepairOrderExportService orderExportService;

    @Autowired
    private RepairOrderService repairOrderService;

    @Autowired
    private RedisTemplate<String, Object> redis;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<AiAssistantSessionVO> listSessions() {
        assertAssistantRole();
        Long userId = SecurityUtils.current().userId();
        List<AiAssistantSessionRecord> rows = sessionStore.listActiveSessions(userId);
        List<AiAssistantSessionVO> result = new ArrayList<>();
        for (AiAssistantSessionRecord row : rows) {
            AiAssistantSessionVO vo = new AiAssistantSessionVO();
            vo.setSessionId(row.getSessionId());
            vo.setTitle(resolveTitle(row));
            vo.setUpdateTime(row.getUpdateTime());
            vo.setPreview(sessionStore.latestPreview(row.getSessionId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    public Long createSession() {
        assertAssistantRole();
        AiAssistantSessionRecord session = sessionStore.createSession(SecurityUtils.current().userId(), "新对话");
        return session.getSessionId();
    }

    @Override
    public List<AiAssistantHistoryMessageVO> listMessages(Long sessionId) {
        assertAssistantRole();
        AiAssistantSessionRecord session = sessionStore.requireOwnedSession(SecurityUtils.current().userId(), sessionId);
        List<AiAssistantMessageRecord> rows = sessionStore.listMessages(session.getSessionId());
        List<AiAssistantHistoryMessageVO> result = new ArrayList<>();
        for (AiAssistantMessageRecord row : rows) {
            result.add(toHistoryMessage(row));
        }
        return result;
    }

    @Override
    public void renameSession(Long sessionId, String title) {
        assertAssistantRole();
        if (title == null || title.isBlank()) {
            throw BusinessException.badRequest("title 不能为空");
        }
        AiAssistantSessionRecord session = sessionStore.requireOwnedSession(SecurityUtils.current().userId(), sessionId);
        session.setTitle(truncateTitle(title.trim()));
        sessionStore.saveSession(session);
    }

    @Override
    public void deleteSession(Long sessionId) {
        assertAssistantRole();
        sessionStore.logicalDelete(SecurityUtils.current().userId(), sessionId);
    }

    @Override
    public AiAssistantMessageVO chat(AiAssistantChatDTO body) {
        if (body == null || body.getMessage() == null || body.getMessage().isBlank()) {
            throw BusinessException.badRequest("message 不能为空");
        }
        String role = assertAssistantRole();
        String message = body.getMessage().trim();
        AiAssistantSessionRecord session = resolveSession(body.getSessionId());
        List<AiAssistantMessageRecord> history = sessionStore.listRecentMessages(session.getSessionId(), CONTEXT_MESSAGE_LIMIT);
        saveMessage(session.getSessionId(), "USER", message, null);
        touchSession(session, message);
        AiAssistantMessageVO vo = new AiAssistantMessageVO();
        vo.setSessionId(session.getSessionId());
        IntentResult intent = resolveIntent(message, role, history);
        switch (intent.intent) {
            case EXPORT -> handleExport(message, history, intent, vo);
            case ORDER_SEARCH -> handleOrderSearch(message, role, vo);
            case ASSET_SEARCH -> handleAssetSearch(message, role, vo);
            case SUGGESTION_SEARCH -> handleSuggestionSearch(message, role, vo);
            default -> handleStatistics(intent, role, vo, message);
        }
        Map<String, Object> extra = buildExtra(vo);
        saveMessage(session.getSessionId(), "ASSISTANT", vo.getReplyText(), extra);
        return vo;
    }

    @Override
    public void confirmExport(AiExportConfirmDTO body, HttpServletResponse response) {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可确认导出");
        }
        if (body == null || body.getPreviewToken() == null || body.getPreviewToken().isBlank()) {
            throw BusinessException.badRequest("previewToken 不能为空");
        }
        Object cached = redis.opsForValue().get(previewKey(body.getPreviewToken()));
        if (cached == null) {
            throw BusinessException.conflict("导出预览已过期，请重新生成");
        }
        try {
            AiExportPreviewCacheDTO payload = objectMapper.convertValue(cached, AiExportPreviewCacheDTO.class);
            String exportType = payload.getExportType();
            String rangeType = payload.getRangeType();
            if ("ORDER".equals(exportType)) {
                if (payload.getOrderQuery() == null) {
                    throw BusinessException.badRequest("导出条件已失效，请重新生成预览");
                }
                orderExportService.export(payload.getOrderQuery(), response);
            } else if ("STATISTICS".equals(exportType)) {
                StatisticsQueryDTO query = new StatisticsQueryDTO();
                query.setRangeType(rangeType);
                statisticsExportService.export(query, response);
            } else {
                throw BusinessException.badRequest("未知导出类型");
            }
            redis.delete(previewKey(body.getPreviewToken()));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BusinessException.badRequest("导出执行失败");
        }
    }

    private void handleStatistics(IntentResult intent, String role, AiAssistantMessageVO vo, String message) {
        String rangeType = intent.rangeType != null ? intent.rangeType : StatisticsRangeType.LAST_30_DAYS.getCode();
        AiStatisticsQueryResultVO statVo = new AiStatisticsQueryResultVO();
        statVo.setResultType("ADMIN".equals(role) ? "ADMIN_OVERVIEW" : "REPAIRER_PERSONAL");
        List<AiDynamicChartVO> charts = dynamicChartService.buildCharts(role, rangeType, intent.chartType,
                intent.dimension, message);
        statVo.setCharts(charts);
        StatisticsRangeType range = StatisticsRangeType.of(rangeType);
        AiDynamicChartVO primary = charts.isEmpty() ? null : charts.get(0);
        String defaultSummary = primary == null
                ? "已为您查询 " + range.getLabel() + " 统计数据。"
                : "已为您绘制 " + primary.getTitle() + "。";
        statVo.setSummary(intent.summary != null && !intent.summary.isBlank() ? intent.summary : defaultSummary);
        vo.setStatisticsResult(statVo);
        vo.setReplyText(statVo.getSummary());
    }

    private void handleExport(String message, List<AiAssistantMessageRecord> history, IntentResult intent, AiAssistantMessageVO vo) {
        String exportType = intent.exportType;
        String rangeType = intent.rangeType != null ? intent.rangeType : StatisticsRangeType.LAST_30_DAYS.getCode();
        if (exportType == null) {
            vo.setOutOfScope(true);
            vo.setReplyText("导出描述不够明确，请说明导出类型（工单/统计）和时间范围。");
            return;
        }
        if ("STATISTICS".equals(exportType)) {
            buildStatisticsExportPreview(intent, rangeType, vo);
            return;
        }
        AiNlExportParseResultDTO parsed = exportParseService.parse(message, buildPromptWithHistory(history, message),
                SecurityUtils.current().userId());
        if (!parsed.isSuccess()) {
            vo.setOutOfScope(true);
            vo.setReplyText(parsed.getErrorMessage());
            return;
        }
        PageResult<?> countPage = repairOrderService.search(1, 1, parsed.getQuery());
        int estimatedCount = (int) countPage.total();
        AiExportPreviewVO preview = new AiExportPreviewVO();
        preview.setPreviewToken(UUID.randomUUID().toString().replace("-", ""));
        preview.setExportType("ORDER");
        preview.setFilterSummary(parsed.getFilterSummary());
        preview.setPreviewSummary("将导出符合以下条件的工单：" + parsed.getFilterSummary());
        preview.setEstimatedCount(estimatedCount);
        preview.setExpireSeconds(600);
        if (estimatedCount <= 0) {
            preview.setConfirmDisabled(true);
            preview.setConfirmDisabledReason("预计导出数量为 0，无法确认导出。");
        } else if (estimatedCount > 1000) {
            preview.setConfirmDisabled(true);
            preview.setConfirmDisabledReason("预计导出数量超过 1000 条，请缩小筛选范围。");
        } else {
            preview.setConfirmDisabled(false);
        }
        storePreview(preview.getPreviewToken(), "ORDER", rangeType, parsed.getQuery());
        vo.setExportPreview(preview);
        vo.setReplyText((intent.summary != null && !intent.summary.isBlank() ? intent.summary : preview.getPreviewSummary())
                + " 预计 " + estimatedCount + " 条。");
    }

    private void buildStatisticsExportPreview(IntentResult intent, String rangeType, AiAssistantMessageVO vo) {
        String token = UUID.randomUUID().toString().replace("-", "");
        AiExportPreviewVO preview = new AiExportPreviewVO();
        preview.setPreviewToken(token);
        preview.setExportType("STATISTICS");
        preview.setFilterSummary(StatisticsRangeType.of(rangeType).getLabel());
        preview.setPreviewSummary("将导出 " + StatisticsRangeType.of(rangeType).getLabel() + " 管理统计数据。");
        preview.setEstimatedCount(1);
        preview.setConfirmDisabled(false);
        preview.setExpireSeconds(600);
        storePreview(token, "STATISTICS", rangeType, null);
        vo.setExportPreview(preview);
        vo.setReplyText(intent.summary != null && !intent.summary.isBlank() ? intent.summary : preview.getPreviewSummary());
    }

    private void handleOrderSearch(String message, String role, AiAssistantMessageVO vo) {
        if (!"ADMIN".equals(role)) {
            vo.setOutOfScope(true);
            vo.setReplyText("仅管理员可搜索历史工单。");
            return;
        }
        if (!embeddingGateway.isAvailable()) {
            vo.setOrderSearchResult(semanticSearchService.fallbackSearchOrders(message, new RepairOrderQueryDTO(),
                    SecurityUtils.current().userId()));
            vo.setReplyText("Embedding 暂不可用，已按关键词降级搜索。");
            return;
        }
        vo.setOrderSearchResult(semanticSearchService.searchOrders(message, new RepairOrderQueryDTO(),
                SecurityUtils.current().userId()));
        vo.setReplyText(vo.getOrderSearchResult().getSummary());
    }

    private void handleAssetSearch(String message, String role, AiAssistantMessageVO vo) {
        if (!"ADMIN".equals(role) && !"REPAIRER".equals(role)) {
            vo.setOutOfScope(true);
            vo.setReplyText("当前角色不可搜索资产。");
            return;
        }
        vo.setAssetSearchResult(semanticSearchService.searchAssets(message, new AssetQueryDTO(),
                SecurityUtils.current().userId(), role));
        vo.setReplyText(vo.getAssetSearchResult().getSummary());
    }

    private void handleSuggestionSearch(String message, String role, AiAssistantMessageVO vo) {
        vo.setSuggestionSearchResult(semanticSearchService.searchSuggestions(message,
                SecurityUtils.current().userId(), role));
        vo.setReplyText(vo.getSuggestionSearchResult().getSummary());
    }

    private IntentResult resolveIntent(String message, String role, List<AiAssistantMessageRecord> history) {
        IntentResult fallback = fallbackIntent(message, role);
        AiCallResult result = aiGateway.chat(AiSceneType.NL_STATISTICS, SecurityUtils.current().userId(), null, "AI_ASSISTANT",
                modelRoute.chatModel(), UNIFIED_SYSTEM, buildPromptWithHistory(history, message));
        if (!result.isSuccess()) {
            return fallback;
        }
        IntentResult parsed = parseIntentJson(result.getContent(), role);
        return parsed != null ? parsed : fallback;
    }

    private IntentResult parseIntentJson(String content, String role) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            String json = extractJson(content);
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {
            });
            IntentResult intent = new IntentResult();
            String intentCode = String.valueOf(map.getOrDefault("intent", "STATISTICS")).toUpperCase();
            intent.intent = resolveIntentType(intentCode, role);
            intent.rangeType = normalizeRangeType(String.valueOf(map.getOrDefault("rangeType", "")));
            Object exportType = map.get("exportType");
            intent.exportType = exportType == null ? null : String.valueOf(exportType).toUpperCase();
            Object summary = map.get("summary");
            intent.summary = summary == null ? null : String.valueOf(summary);
            Object searchQuery = map.get("searchQuery");
            intent.searchQuery = searchQuery == null ? null : String.valueOf(searchQuery);
            Object chartType = map.get("chartType");
            intent.chartType = chartType == null ? null : String.valueOf(chartType).toUpperCase();
            Object dimension = map.get("dimension");
            intent.dimension = dimension == null ? null : String.valueOf(dimension).toUpperCase();
            if (intent.intent == AiAssistantSceneType.EXPORT && intent.exportType == null) {
                intent.exportType = parseExportType(content);
            }
            return intent;
        } catch (Exception ex) {
            return null;
        }
    }

    private AiAssistantSceneType resolveIntentType(String intentCode, String role) {
        return switch (intentCode) {
            case "EXPORT" -> "ADMIN".equals(role) ? AiAssistantSceneType.EXPORT : AiAssistantSceneType.STATISTICS;
            case "ORDER_SEARCH" -> "ADMIN".equals(role) ? AiAssistantSceneType.ORDER_SEARCH : AiAssistantSceneType.STATISTICS;
            case "ASSET_SEARCH" -> ("ADMIN".equals(role) || "REPAIRER".equals(role))
                    ? AiAssistantSceneType.ASSET_SEARCH : AiAssistantSceneType.STATISTICS;
            case "SUGGESTION_SEARCH" -> ("ADMIN".equals(role) || "REPAIRER".equals(role))
                    ? AiAssistantSceneType.SUGGESTION_SEARCH : AiAssistantSceneType.STATISTICS;
            default -> AiAssistantSceneType.STATISTICS;
        };
    }

    private IntentResult fallbackIntent(String message, String role) {
        IntentResult intent = new IntentResult();
        if ("ADMIN".equals(role) && looksLikeExport(message)) {
            intent.intent = AiAssistantSceneType.EXPORT;
        } else if ("ADMIN".equals(role) && looksLikeOrderSearch(message)) {
            intent.intent = AiAssistantSceneType.ORDER_SEARCH;
        } else if (looksLikeAssetSearch(message) && ("ADMIN".equals(role) || "REPAIRER".equals(role))) {
            intent.intent = AiAssistantSceneType.ASSET_SEARCH;
        } else if (looksLikeSuggestionSearch(message) && ("ADMIN".equals(role) || "REPAIRER".equals(role))) {
            intent.intent = AiAssistantSceneType.SUGGESTION_SEARCH;
        } else {
            intent.intent = AiAssistantSceneType.STATISTICS;
        }
        intent.rangeType = parseRangeType(message);
        intent.exportType = intent.intent == AiAssistantSceneType.EXPORT ? parseExportType(message) : null;
        intent.chartType = AiChartType.fromMessage(message).getCode();
        intent.dimension = AiChartDimension.fromMessage(role, message).getCode();
        intent.searchQuery = message;
        intent.summary = null;
        return intent;
    }

    private boolean looksLikeExport(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("导出") || message.contains("下载") || message.toLowerCase().contains("excel");
    }

    private boolean looksLikeOrderSearch(String message) {
        return message != null && (message.contains("搜索工单") || message.contains("查找工单") || message.contains("历史工单"));
    }

    private boolean looksLikeAssetSearch(String message) {
        return message != null && (message.contains("搜索资产") || message.contains("查找资产"));
    }

    private boolean looksLikeSuggestionSearch(String message) {
        return message != null && (message.contains("搜索建议") || message.contains("查找建议"));
    }

    private String buildPromptWithHistory(List<AiAssistantMessageRecord> history, String message) {
        StringBuilder builder = new StringBuilder();
        if (!history.isEmpty()) {
            builder.append("对话历史：\n");
            for (AiAssistantMessageRecord item : history) {
                builder.append(roleLabel(item.getRole())).append("：").append(item.getContentSummary()).append("\n");
            }
            builder.append("\n");
        }
        builder.append("当前用户输入：").append(message);
        return builder.toString();
    }

    private String roleLabel(String role) {
        return "USER".equalsIgnoreCase(role) ? "用户" : "助手";
    }

    private AiAssistantSessionRecord resolveSession(Long sessionId) {
        Long userId = SecurityUtils.current().userId();
        if (sessionId != null) {
            try {
                return sessionStore.requireOwnedSession(userId, sessionId);
            } catch (BusinessException ignored) {
                // 会话无效时自动新建
            }
        }
        return sessionStore.createSession(userId, null);
    }

    private void touchSession(AiAssistantSessionRecord session, String firstMessageHint) {
        if (session.getTitle() == null || session.getTitle().isBlank() || "新对话".equals(session.getTitle())) {
            session.setTitle(truncateTitle(firstMessageHint));
        }
        sessionStore.saveSession(session);
    }

    private String assertAssistantRole() {
        String role = SecurityUtils.current().roleCode();
        if ("STUDENT".equals(role) || "TEACHER".equals(role)) {
            throw BusinessException.forbidden("当前角色不可使用 AI 助手");
        }
        return role;
    }

    private void saveMessage(Long sessionId, String role, String content, Map<String, Object> extra) {
        AiAssistantMessageRecord message = new AiAssistantMessageRecord();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContentSummary(truncate(content, CONTENT_MAX_LENGTH));
        if (extra != null && !extra.isEmpty()) {
            try {
                message.setExtraJson(objectMapper.writeValueAsString(extra));
            } catch (Exception ex) {
                throw BusinessException.badRequest("消息附加数据序列化失败");
            }
        }
        sessionStore.appendMessage(message);
    }

    private Map<String, Object> buildExtra(AiAssistantMessageVO vo) {
        Map<String, Object> extra = new LinkedHashMap<>();
        if (vo.getStatisticsResult() != null) {
            extra.put("statisticsResult", vo.getStatisticsResult());
        }
        if (vo.getExportPreview() != null) {
            extra.put("exportPreview", vo.getExportPreview());
        }
        if (vo.getOrderSearchResult() != null) {
            extra.put("orderSearchResult", vo.getOrderSearchResult());
        }
        if (vo.getAssetSearchResult() != null) {
            extra.put("assetSearchResult", vo.getAssetSearchResult());
        }
        if (vo.getSuggestionSearchResult() != null) {
            extra.put("suggestionSearchResult", vo.getSuggestionSearchResult());
        }
        if (Boolean.TRUE.equals(vo.getOutOfScope())) {
            extra.put("outOfScope", true);
        }
        return extra.isEmpty() ? null : extra;
    }

    private AiAssistantHistoryMessageVO toHistoryMessage(AiAssistantMessageRecord row) {
        AiAssistantHistoryMessageVO vo = new AiAssistantHistoryMessageVO();
        vo.setMessageId(row.getMessageId());
        vo.setRole(row.getRole());
        vo.setText(row.getContentSummary());
        vo.setCreateTime(row.getCreateTime());
        if (row.getExtraJson() != null && !row.getExtraJson().isBlank()) {
            try {
                Map<String, Object> extra = objectMapper.readValue(row.getExtraJson(), new TypeReference<>() {
                });
                if (extra.get("statisticsResult") != null) {
                    vo.setStatisticsResult(objectMapper.convertValue(extra.get("statisticsResult"), AiStatisticsQueryResultVO.class));
                }
                if (extra.get("exportPreview") != null) {
                    vo.setExportPreview(objectMapper.convertValue(extra.get("exportPreview"), AiExportPreviewVO.class));
                }
                if (extra.get("orderSearchResult") != null) {
                    vo.setOrderSearchResult(objectMapper.convertValue(extra.get("orderSearchResult"), AiOrderSearchResultVO.class));
                }
                if (extra.get("assetSearchResult") != null) {
                    vo.setAssetSearchResult(objectMapper.convertValue(extra.get("assetSearchResult"), AiAssetSearchResultVO.class));
                }
                if (extra.get("suggestionSearchResult") != null) {
                    vo.setSuggestionSearchResult(objectMapper.convertValue(extra.get("suggestionSearchResult"), AiSuggestionSearchResultVO.class));
                }
            } catch (Exception ignored) {
                // 历史附加数据损坏时仍展示文本
            }
        }
        return vo;
    }

    private String resolveTitle(AiAssistantSessionRecord session) {
        if (session.getTitle() != null && !session.getTitle().isBlank()) {
            return session.getTitle();
        }
        return "新对话";
    }

    private void storePreview(String token, String exportType, String rangeType, RepairOrderQueryDTO orderQuery) {
        redis.opsForValue().set(previewKey(token), new AiExportPreviewCacheDTO(exportType, rangeType, orderQuery), 600, TimeUnit.SECONDS);
    }

    private String normalizeRangeType(String raw) {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw)) {
            return StatisticsRangeType.LAST_30_DAYS.getCode();
        }
        return parseRangeType(raw);
    }

    private String parseRangeType(String content) {
        if (content == null) {
            return StatisticsRangeType.LAST_30_DAYS.getCode();
        }
        String upper = content.toUpperCase();
        for (StatisticsRangeType type : StatisticsRangeType.values()) {
            if (upper.contains(type.getCode())) {
                return type.getCode();
            }
        }
        if (content.contains("七天") || content.contains("7天") || content.contains("本周") || content.contains("上周")) {
            return StatisticsRangeType.LAST_7_DAYS.getCode();
        }
        if (content.contains("九十天") || content.contains("90天") || content.contains("季度")) {
            return StatisticsRangeType.LAST_90_DAYS.getCode();
        }
        if (content.contains("年度") || content.contains("今年")) {
            return StatisticsRangeType.CURRENT_YEAR.getCode();
        }
        return StatisticsRangeType.LAST_30_DAYS.getCode();
    }

    private String parseExportType(String content) {
        if (content == null) {
            return null;
        }
        String text = content.toUpperCase();
        if (text.contains("ORDER") || content.contains("工单")) {
            return "ORDER";
        }
        if (text.contains("STATISTICS") || content.contains("统计")) {
            return "STATISTICS";
        }
        return null;
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) : text;
    }

    private String truncateTitle(String text) {
        String value = text == null ? "新对话" : text.trim();
        if (value.isBlank()) {
            return "新对话";
        }
        return value.length() > 24 ? value.substring(0, 24) + "…" : value;
    }

    private String previewKey(String token) {
        return "ai:export:preview:" + token;
    }

    private static final class IntentResult {
        private AiAssistantSceneType intent = AiAssistantSceneType.STATISTICS;
        private String rangeType;
        private String exportType;
        private String searchQuery;
        private String summary;
        private String chartType;
        private String dimension;
    }
}
