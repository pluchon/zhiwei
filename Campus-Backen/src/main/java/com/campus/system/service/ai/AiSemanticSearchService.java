package com.campus.system.service.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.enums.AiSceneType;
import com.campus.system.common.enums.SuggestionStatus;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.dto.AssetQueryDTO;
import com.campus.system.dto.RepairOrderQueryDTO;
import com.campus.system.entity.RepairerSuggestion;
import com.campus.system.mapper.RepairerSuggestionMapper;
import com.campus.system.service.interfaces.AssetService;
import com.campus.system.service.interfaces.RepairOrderService;
import com.campus.system.vo.AiAssetSearchItemVO;
import com.campus.system.vo.AiAssetSearchResultVO;
import com.campus.system.vo.AiOrderSearchItemVO;
import com.campus.system.vo.AiOrderSearchResultVO;
import com.campus.system.vo.AiSuggestionSearchItemVO;
import com.campus.system.vo.AiSuggestionSearchResultVO;
import com.campus.system.vo.AssetVO;
import com.campus.system.vo.RepairOrderVO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// AI 助手语义搜索实现
@Component
public class AiSemanticSearchService {

    private static final int ORDER_CANDIDATE_LIMIT = 300;
    private static final int SEARCH_RESULT_LIMIT = 10;

    @Autowired
    private AiEmbeddingGateway embeddingGateway;

    @Autowired
    private RepairOrderService repairOrderService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private RepairerSuggestionMapper suggestionMapper;

    // 管理员历史工单语义搜索
    public AiOrderSearchResultVO searchOrders(String queryText, RepairOrderQueryDTO scope, Long operatorId) {
        RepairOrderQueryDTO safeScope = scope == null ? new RepairOrderQueryDTO() : scope;
        PageResult<RepairOrderVO> page = repairOrderService.search(1, ORDER_CANDIDATE_LIMIT, safeScope);
        List<RepairOrderVO> candidates = page.records();
        if (candidates.isEmpty()) {
            AiOrderSearchResultVO vo = new AiOrderSearchResultVO();
            vo.setSummary("未找到符合条件的工单。");
            vo.setItems(List.of());
            return vo;
        }
        List<RepairOrderVO> ranked = embeddingGateway.rankBySimilarity(AiSceneType.ORDER_SEMANTIC_SEARCH, operatorId,
                queryText, candidates, item -> nullSafe(item.getTitle()) + " " + nullSafe(item.getDescription()));
        List<AiOrderSearchItemVO> items = new ArrayList<>();
        for (RepairOrderVO order : ranked.stream().limit(SEARCH_RESULT_LIMIT).toList()) {
            items.add(toOrderItem(order));
        }
        AiOrderSearchResultVO vo = new AiOrderSearchResultVO();
        vo.setSummary("已为您找到 " + items.size() + " 条相关工单。");
        vo.setItems(items);
        return vo;
    }

    // 管理员/维修师傅资产语义搜索
    public AiAssetSearchResultVO searchAssets(String queryText, AssetQueryDTO scope, Long operatorId, String roleCode) {
        if (!"ADMIN".equals(roleCode) && !"REPAIRER".equals(roleCode)) {
            throw BusinessException.forbidden("当前角色不可搜索资产");
        }
        AssetQueryDTO safeScope = scope == null ? new AssetQueryDTO() : scope;
        PageResult<AssetVO> page = assetService.search(1, ORDER_CANDIDATE_LIMIT, safeScope);
        List<AssetVO> candidates = page.records();
        if (candidates.isEmpty()) {
            AiAssetSearchResultVO vo = new AiAssetSearchResultVO();
            vo.setSummary("未找到符合条件的资产。");
            vo.setItems(List.of());
            return vo;
        }
        List<AssetVO> ranked = embeddingGateway.rankBySimilarity(AiSceneType.ASSET_SEMANTIC_SEARCH, operatorId,
                queryText, candidates, item -> nullSafe(item.getAssetName()) + " "
                        + nullSafe(item.getAssetCategoryName()) + " " + nullSafe(item.getDescription()));
        List<AiAssetSearchItemVO> items = new ArrayList<>();
        for (AssetVO asset : ranked.stream().limit(SEARCH_RESULT_LIMIT).toList()) {
            items.add(toAssetItem(asset));
        }
        AiAssetSearchResultVO vo = new AiAssetSearchResultVO();
        vo.setSummary("已为您找到 " + items.size() + " 条相关资产。");
        vo.setItems(items);
        return vo;
    }

    // 建议语义搜索
    public AiSuggestionSearchResultVO searchSuggestions(String queryText, Long operatorId, String roleCode) {
        List<RepairerSuggestion> candidates;
        if ("ADMIN".equals(roleCode)) {
            candidates = suggestionMapper.selectList(Wrappers.<RepairerSuggestion>lambdaQuery()
                    .eq(RepairerSuggestion::getDeleteState, 0).eq(RepairerSuggestion::getWithdrawnFlag, 0)
                    .orderByDesc(RepairerSuggestion::getCreateTime).last("limit " + ORDER_CANDIDATE_LIMIT));
        } else if ("REPAIRER".equals(roleCode)) {
            candidates = suggestionMapper.selectList(Wrappers.<RepairerSuggestion>lambdaQuery()
                    .eq(RepairerSuggestion::getDeleteState, 0).eq(RepairerSuggestion::getRepairerId, operatorId)
                    .orderByDesc(RepairerSuggestion::getCreateTime).last("limit " + ORDER_CANDIDATE_LIMIT));
        } else {
            throw BusinessException.forbidden("当前角色不可搜索建议");
        }
        if (candidates.isEmpty()) {
            AiSuggestionSearchResultVO vo = new AiSuggestionSearchResultVO();
            vo.setSummary("未找到相关建议。");
            vo.setItems(List.of());
            return vo;
        }
        List<RepairerSuggestion> ranked = embeddingGateway.rankBySimilarity(AiSceneType.SUGGESTION_SIMILARITY, operatorId,
                queryText, candidates, item -> nullSafe(item.getTitle()) + " " + nullSafe(item.getContent()));
        List<AiSuggestionSearchItemVO> items = new ArrayList<>();
        for (RepairerSuggestion suggestion : ranked.stream().limit(SEARCH_RESULT_LIMIT).toList()) {
            items.add(toSuggestionItem(suggestion));
        }
        AiSuggestionSearchResultVO vo = new AiSuggestionSearchResultVO();
        vo.setSummary("已为您找到 " + items.size() + " 条相关建议。");
        vo.setItems(items);
        return vo;
    }

    // Embedding 不可用时按关键词降级检索工单
    public AiOrderSearchResultVO fallbackSearchOrders(String queryText, RepairOrderQueryDTO scope, Long operatorId) {
        RepairOrderQueryDTO safeScope = scope == null ? new RepairOrderQueryDTO() : scope;
        if (safeScope.getTitleKeyword() == null || safeScope.getTitleKeyword().isBlank()) {
            safeScope.setTitleKeyword(queryText);
        }
        return searchOrders(queryText, safeScope, operatorId);
    }

    private AiOrderSearchItemVO toOrderItem(RepairOrderVO order) {
        AiOrderSearchItemVO item = new AiOrderSearchItemVO();
        item.setOrderId(order.getOrderId());
        item.setOrderNo(order.getOrderNo());
        item.setTitle(order.getTitle());
        item.setStatus(order.getStatus());
        item.setStatusLabel(statusLabel(order.getStatus()));
        item.setLocationSummary(buildOrderLocation(order));
        return item;
    }

    private AiAssetSearchItemVO toAssetItem(AssetVO asset) {
        AiAssetSearchItemVO item = new AiAssetSearchItemVO();
        item.setAssetId(asset.getAssetId());
        item.setAssetNo(asset.getAssetNo());
        item.setAssetName(asset.getAssetName());
        item.setCategoryName(asset.getAssetCategoryName());
        item.setLocationSummary(buildAssetLocation(asset));
        return item;
    }

    private AiSuggestionSearchItemVO toSuggestionItem(RepairerSuggestion suggestion) {
        AiSuggestionSearchItemVO item = new AiSuggestionSearchItemVO();
        item.setSuggestionId(suggestion.getSuggestionId());
        item.setTitle(suggestion.getTitle());
        item.setStatus(suggestion.getStatus());
        item.setStatusLabel(suggestionStatusLabel(suggestion.getStatus()));
        item.setCategory(suggestion.getCategory());
        return item;
    }

    private String buildOrderLocation(RepairOrderVO order) {
        StringBuilder builder = new StringBuilder();
        if (order.getCampus() != null) {
            builder.append(order.getCampus());
        }
        if (order.getBuilding() != null) {
            builder.append(" ").append(order.getBuilding());
        }
        return builder.toString().trim();
    }

    private String buildAssetLocation(AssetVO asset) {
        StringBuilder builder = new StringBuilder();
        if (asset.getCampusName() != null) {
            builder.append(asset.getCampusName());
        }
        if (asset.getBuildingName() != null) {
            builder.append(" ").append(asset.getBuildingName());
        }
        return builder.toString().trim();
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

    private String suggestionStatusLabel(String status) {
        if (status == null) {
            return "未知";
        }
        try {
            return SuggestionStatus.of(status).getLabel();
        } catch (Exception ex) {
            return status;
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
