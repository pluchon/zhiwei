package com.campus.system.service.asset;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.entity.AssetCategory;
import com.campus.system.mapper.AssetCategoryMapper;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 资产分类模糊匹配服务
@Component
public class AssetCategoryFuzzyMatchService {

    @Autowired
    private AssetCategoryMapper categoryMapper;

    // 将 AI 识别出的分类文本映射到已启用分类
    public Long matchCategoryId(String categoryText) {
        if (categoryText == null || categoryText.isBlank()) {
            return null;
        }
        String normalized = categoryText.trim();
        List<AssetCategory> categories = categoryMapper.selectList(Wrappers.<AssetCategory>lambdaQuery()
                .eq(AssetCategory::getDeleteState, 0).eq(AssetCategory::getStatus, 0));
        if (categories.isEmpty()) {
            return null;
        }
        for (AssetCategory category : categories) {
            if (normalized.equalsIgnoreCase(category.getCategoryName())) {
                return category.getAssetCategoryId();
            }
        }
        for (AssetCategory category : categories) {
            String name = category.getCategoryName();
            if (normalized.contains(name) || name.contains(normalized)) {
                return category.getAssetCategoryId();
            }
        }
        return categories.stream().min(Comparator.comparingInt(c -> levenshtein(normalized.toLowerCase(), c.getCategoryName().toLowerCase())))
                .filter(c -> levenshtein(normalized.toLowerCase(), c.getCategoryName().toLowerCase()) <= Math.max(2, normalized.length() / 3))
                .map(AssetCategory::getAssetCategoryId).orElse(null);
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
