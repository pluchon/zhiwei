package com.campus.system.service.asset;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.enums.AiSceneType;
import com.campus.system.entity.AssetCategory;
import com.campus.system.mapper.AssetCategoryMapper;
import com.campus.system.service.ai.AiEmbeddingGateway;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 资产分类语义匹配，失败时降级到字符串模糊匹配
@Component
public class AssetCategorySemanticMatchService {

    @Autowired
    private AssetCategoryMapper categoryMapper;

    @Autowired
    private AssetCategoryFuzzyMatchService fuzzyMatchService;

    @Autowired
    private AiEmbeddingGateway embeddingGateway;

    // 将 AI 识别出的分类文本映射到已启用分类
    public Long matchCategoryId(String categoryText, Long operatorId) {
        if (categoryText == null || categoryText.isBlank()) {
            return null;
        }
        List<AssetCategory> categories = categoryMapper.selectList(Wrappers.<AssetCategory>lambdaQuery()
                .eq(AssetCategory::getDeleteState, 0).eq(AssetCategory::getStatus, 0));
        if (categories.isEmpty()) {
            return null;
        }
        if (embeddingGateway.isAvailable()) {
            AssetCategory matched = embeddingGateway.matchBest(AiSceneType.ASSET_CATEGORY_MATCH, operatorId,
                    categoryText.trim(), categories, AssetCategory::getCategoryName);
            if (matched != null) {
                return matched.getAssetCategoryId();
            }
            return null;
        }
        return fuzzyMatchService.matchCategoryId(categoryText);
    }
}
