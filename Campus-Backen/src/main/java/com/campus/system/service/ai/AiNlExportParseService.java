package com.campus.system.service.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.enums.AiSceneType;
import com.campus.system.common.enums.RepairStatus;
import com.campus.system.dto.AiNlExportFilterDTO;
import com.campus.system.dto.AiNlExportParseResultDTO;
import com.campus.system.dto.RepairOrderQueryDTO;
import com.campus.system.entity.Building;
import com.campus.system.entity.Campus;
import com.campus.system.entity.RepairCategory;
import com.campus.system.mapper.BuildingMapper;
import com.campus.system.mapper.CampusMapper;
import com.campus.system.mapper.RepairCategoryMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 自然语言工单导出条件解析与校验
@Component
public class AiNlExportParseService {

    private static final String SYSTEM_PROMPT = """
            你是校园报修工单导出条件解析助手。结合对话历史理解用户意图，仅返回 JSON：
            {
              "createTimeFrom":"yyyy-MM-dd 或 null",
              "createTimeTo":"yyyy-MM-dd 或 null",
              "status":整数或null,
              "categoryName":"故障类型名称或null",
              "campusName":"校区名称或null",
              "buildingName":"楼栋名称或null",
              "orderNo":"工单编号或null",
              "titleKeyword":"标题关键词或null",
              "assetNo":"资产编号或null",
              "assetNameKeyword":"资产名称关键词或null",
              "suspectedDuplicate":0/1/null,
              "exportedFlag":0/1/null,
              "longStagnant":true/false/null,
              "unsupportedReason":"若用户要求报修人/维修师傅/完成时间/多个指定工单/复杂排除条件，填写原因，否则 null"
            }
            仅解析支持的筛选条件，不支持的条件写入 unsupportedReason。
            """;

    @Autowired
    private AiClientGateway aiGateway;

    @Autowired
    private AiModelRouteService modelRoute;

    @Autowired
    private RepairCategoryMapper categories;

    @Autowired
    private CampusMapper campusMapper;

    @Autowired
    private BuildingMapper buildingMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // 解析并校验导出条件，失败时返回错误提示
    public AiNlExportParseResultDTO parse(String message, String historyPrompt, Long operatorId) {
        AiCallResult result = aiGateway.chat(AiSceneType.NL_EXPORT, operatorId, null, "AI_ASSISTANT",
                modelRoute.chatModel(), SYSTEM_PROMPT, historyPrompt + "\n当前用户输入：" + message);
        Map<String, Object> raw = parseJson(result);
        if (raw == null) {
            return AiNlExportParseResultDTO.error("导出条件解析失败，请补充创建时间、状态或关键词等明确条件。");
        }
        Object unsupported = raw.get("unsupportedReason");
        if (unsupported != null && !"null".equalsIgnoreCase(String.valueOf(unsupported)) && !String.valueOf(unsupported).isBlank()) {
            return AiNlExportParseResultDTO.error(String.valueOf(unsupported));
        }
        AiNlExportFilterDTO filter = toFilter(raw);
        String validationError = validateHasCondition(filter, raw);
        if (validationError != null) {
            return AiNlExportParseResultDTO.error(validationError);
        }
        RepairOrderQueryDTO query = toQuery(filter, raw);
        if (query == null) {
            return AiNlExportParseResultDTO.error("部分筛选条件无法唯一匹配或无效，请改用更明确的描述。");
        }
        return AiNlExportParseResultDTO.success(query, buildFilterSummary(query));
    }

    private Map<String, Object> parseJson(AiCallResult result) {
        if (result == null || !result.isSuccess() || result.getContent() == null) {
            return null;
        }
        try {
            String content = result.getContent();
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return null;
            }
            return objectMapper.readValue(content.substring(start, end + 1), new TypeReference<>() {
            });
        } catch (Exception ex) {
            return null;
        }
    }

    private AiNlExportFilterDTO toFilter(Map<String, Object> raw) {
        AiNlExportFilterDTO filter = new AiNlExportFilterDTO();
        filter.setCreateTimeFrom(parseDate(raw.get("createTimeFrom"), true));
        filter.setCreateTimeTo(parseDate(raw.get("createTimeTo"), false));
        filter.setStatus(parseInteger(raw.get("status")));
        filter.setOrderNo(parseString(raw.get("orderNo")));
        filter.setTitleKeyword(parseString(raw.get("titleKeyword")));
        filter.setAssetNo(parseString(raw.get("assetNo")));
        filter.setAssetNameKeyword(parseString(raw.get("assetNameKeyword")));
        filter.setSuspectedDuplicate(parseInteger(raw.get("suspectedDuplicate")));
        filter.setExportedFlag(parseInteger(raw.get("exportedFlag")));
        Object longStagnant = raw.get("longStagnant");
        if (longStagnant instanceof Boolean bool) {
            filter.setLongStagnant(bool);
        } else if (longStagnant != null && "true".equalsIgnoreCase(String.valueOf(longStagnant))) {
            filter.setLongStagnant(true);
        }
        return filter;
    }

    private RepairOrderQueryDTO toQuery(AiNlExportFilterDTO filter, Map<String, Object> raw) {
        RepairOrderQueryDTO query = new RepairOrderQueryDTO();
        query.setCreateTimeFrom(filter.getCreateTimeFrom());
        query.setCreateTimeTo(filter.getCreateTimeTo());
        query.setStatus(filter.getStatus());
        query.setOrderNo(filter.getOrderNo());
        query.setTitleKeyword(filter.getTitleKeyword());
        query.setAssetNo(filter.getAssetNo());
        query.setAssetNameKeyword(filter.getAssetNameKeyword());
        query.setSuspectedDuplicate(filter.getSuspectedDuplicate());
        query.setExportedFlag(filter.getExportedFlag());
        query.setLongStagnant(filter.getLongStagnant());
        Long categoryId = resolveUniqueCategory(parseString(raw.get("categoryName")));
        if (parseString(raw.get("categoryName")) != null && categoryId == null) {
            return null;
        }
        query.setCategoryId(categoryId);
        Long campusId = resolveUniqueCampus(parseString(raw.get("campusName")));
        if (parseString(raw.get("campusName")) != null && campusId == null) {
            return null;
        }
        query.setCampusId(campusId);
        Long buildingId = resolveUniqueBuilding(parseString(raw.get("buildingName")), campusId);
        if (parseString(raw.get("buildingName")) != null && buildingId == null) {
            return null;
        }
        query.setBuildingId(buildingId);
        if (filter.getStatus() != null) {
            try {
                RepairStatus.of(filter.getStatus());
            } catch (Exception ex) {
                return null;
            }
        }
        return query;
    }

    private String validateHasCondition(AiNlExportFilterDTO filter, Map<String, Object> raw) {
        boolean has = filter.getCreateTimeFrom() != null || filter.getCreateTimeTo() != null
                || filter.getStatus() != null || filter.getOrderNo() != null
                || (filter.getTitleKeyword() != null && !filter.getTitleKeyword().isBlank())
                || (filter.getAssetNo() != null && !filter.getAssetNo().isBlank())
                || (filter.getAssetNameKeyword() != null && !filter.getAssetNameKeyword().isBlank())
                || filter.getSuspectedDuplicate() != null || filter.getExportedFlag() != null
                || Boolean.TRUE.equals(filter.getLongStagnant())
                || parseString(raw.get("categoryName")) != null
                || parseString(raw.get("campusName")) != null
                || parseString(raw.get("buildingName")) != null;
        if (!has) {
            return "请至少提供一个有效筛选条件，例如创建时间、状态或关键词。";
        }
        return null;
    }

    private String buildFilterSummary(RepairOrderQueryDTO query) {
        List<String> parts = new ArrayList<>();
        if (query.getCreateTimeFrom() != null || query.getCreateTimeTo() != null) {
            parts.add("创建时间 "
                    + (query.getCreateTimeFrom() != null ? query.getCreateTimeFrom().toLocalDate() : "不限")
                    + " 至 "
                    + (query.getCreateTimeTo() != null ? query.getCreateTimeTo().toLocalDate() : "不限"));
        }
        if (query.getStatus() != null) {
            parts.add("状态 " + statusLabel(query.getStatus()));
        }
        if (query.getCategoryId() != null) {
            RepairCategory category = categories.selectById(query.getCategoryId());
            parts.add("故障类型 " + (category != null ? category.getCategoryName() : query.getCategoryId()));
        }
        if (query.getCampusId() != null) {
            Campus campus = campusMapper.selectById(query.getCampusId());
            parts.add("校区 " + (campus != null ? campus.getCampusName() : query.getCampusId()));
        }
        if (query.getBuildingId() != null) {
            Building building = buildingMapper.selectById(query.getBuildingId());
            parts.add("楼栋 " + (building != null ? building.getBuildingName() : query.getBuildingId()));
        }
        if (query.getOrderNo() != null) {
            parts.add("工单编号 " + query.getOrderNo());
        }
        if (query.getTitleKeyword() != null) {
            parts.add("标题含 " + query.getTitleKeyword());
        }
        if (query.getAssetNo() != null) {
            parts.add("资产编号 " + query.getAssetNo());
        }
        if (query.getAssetNameKeyword() != null) {
            parts.add("资产名称含 " + query.getAssetNameKeyword());
        }
        if (query.getSuspectedDuplicate() != null) {
            parts.add(query.getSuspectedDuplicate() == 1 ? "疑似重复" : "非疑似重复");
        }
        if (query.getExportedFlag() != null) {
            parts.add(query.getExportedFlag() == 1 ? "已导出" : "未导出");
        }
        if (Boolean.TRUE.equals(query.getLongStagnant())) {
            parts.add("长时间未进展");
        }
        return String.join("；", parts);
    }

    private Long resolveUniqueCategory(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        List<RepairCategory> rows = categories.selectList(Wrappers.<RepairCategory>lambdaQuery()
                .eq(RepairCategory::getDeleteState, 0).eq(RepairCategory::getCategoryName, name.trim()));
        return rows.size() == 1 ? rows.get(0).getCategoryId() : null;
    }

    private Long resolveUniqueCampus(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        List<Campus> rows = campusMapper.selectList(Wrappers.<Campus>lambdaQuery()
                .eq(Campus::getDeleteState, 0).eq(Campus::getCampusName, name.trim()));
        return rows.size() == 1 ? rows.get(0).getCampusId() : null;
    }

    private Long resolveUniqueBuilding(String name, Long campusId) {
        if (name == null || name.isBlank()) {
            return null;
        }
        var wrapper = Wrappers.<Building>lambdaQuery().eq(Building::getDeleteState, 0).eq(Building::getBuildingName, name.trim());
        if (campusId != null) {
            wrapper.eq(Building::getCampusId, campusId);
        }
        List<Building> rows = buildingMapper.selectList(wrapper);
        return rows.size() == 1 ? rows.get(0).getBuildingId() : null;
    }

    private LocalDateTime parseDate(Object value, boolean startOfDay) {
        String text = parseString(value);
        if (text == null) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(text.substring(0, Math.min(10, text.length())));
            return startOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null || "null".equalsIgnoreCase(String.valueOf(value))) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private String parseString(Object value) {
        if (value == null || "null".equalsIgnoreCase(String.valueOf(value))) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "待匹配";
            case 2 -> "待接单";
            case 3 -> "已接单";
            case 4 -> "处理中";
            case 5 -> "待确认";
            case 6 -> "待仲裁";
            case 7 -> "已完成";
            case 8 -> "已驳回";
            case 9 -> "已关闭";
            default -> "未知";
        };
    }
}
