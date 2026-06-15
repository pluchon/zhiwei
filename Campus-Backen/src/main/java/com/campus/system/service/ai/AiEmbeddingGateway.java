package com.campus.system.service.ai;

import com.campus.system.common.config.AiModelProperties;
import com.campus.system.common.enums.AiSceneType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

// 统一 Embedding 调用、缓存与余弦相似度排序
@Component
public class AiEmbeddingGateway {

    private static final Logger log = LoggerFactory.getLogger(AiEmbeddingGateway.class);

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired
    private AiModelProperties properties;

    @Autowired
    private AiAuditService auditService;

    @Autowired
    private RedisTemplate<String, Object> redis;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    // 按 query 文本对候选进行语义排序，失败时返回原始顺序
    public <T> List<T> rankBySimilarity(AiSceneType scene, Long operatorId, String queryText,
            List<T> candidates, java.util.function.Function<T, String> textExtractor) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (!isAvailable() || queryText == null || queryText.isBlank()) {
            return new ArrayList<>(candidates);
        }
        try {
            float[] queryVector = embedText(queryText);
            List<ScoredItem<T>> scored = new ArrayList<>();
            for (T candidate : candidates) {
                String text = textExtractor.apply(candidate);
                if (text == null || text.isBlank()) {
                    scored.add(new ScoredItem<>(candidate, 0));
                    continue;
                }
                float[] vector = embedText(text);
                scored.add(new ScoredItem<>(candidate, cosineSimilarity(queryVector, vector)));
            }
            scored.sort(Comparator.comparingDouble(ScoredItem<T>::score).reversed());
            auditService.recordSuccess(scene, operatorId, "EMBEDDING", null);
            return scored.stream().map(ScoredItem::item).toList();
        } catch (Exception ex) {
            log.warn("Embedding 排序失败 scene={}", scene.getCode(), ex);
            auditService.recordDegraded(scene, operatorId, "EMBEDDING", null, truncate(ex.getMessage()));
            return new ArrayList<>(candidates);
        }
    }

    // 在候选中选取与 query 最相似且超过阈值的项
    public <T> T matchBest(AiSceneType scene, Long operatorId, String queryText, List<T> candidates,
            java.util.function.Function<T, String> textExtractor) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        List<T> ranked = rankBySimilarity(scene, operatorId, queryText, candidates, textExtractor);
        if (ranked.isEmpty()) {
            return null;
        }
        try {
            float[] queryVector = embedText(queryText);
            T best = ranked.get(0);
            String text = textExtractor.apply(best);
            if (text == null || text.isBlank()) {
                return null;
            }
            double score = cosineSimilarity(queryVector, embedText(text));
            if (score >= properties.getEmbeddingSimilarityThreshold()) {
                return best;
            }
            return null;
        } catch (Exception ex) {
            log.warn("Embedding 阈值匹配失败 scene={}", scene.getCode(), ex);
            return null;
        }
    }

    // 检测两段文本是否语义相似
    public boolean isSimilar(AiSceneType scene, Long operatorId, String left, String right) {
        if (!isAvailable() || left == null || left.isBlank() || right == null || right.isBlank()) {
            return false;
        }
        try {
            float[] leftVector = embedText(left);
            float[] rightVector = embedText(right);
            double score = cosineSimilarity(leftVector, rightVector);
            auditService.recordSuccess(scene, operatorId, "EMBEDDING", null);
            return score >= properties.getEmbeddingSimilarityThreshold();
        } catch (Exception ex) {
            log.warn("Embedding 相似度检测失败 scene={}", scene.getCode(), ex);
            auditService.recordDegraded(scene, operatorId, "EMBEDDING", null, truncate(ex.getMessage()));
            return false;
        }
    }

    public boolean isAvailable() {
        return properties.isEnabled() && embeddingModel != null && apiKey != null && !apiKey.isBlank();
    }

    private float[] embedText(String text) throws Exception {
        String cacheKey = buildCacheKey(text);
        try {
            Object cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.convertValue(cached, new TypeReference<>() {
                });
            }
        } catch (Exception ex) {
            // 历史版本曾将 float[] 写为无类型信息的裸数组，读取失败时删除并重新生成缓存。
            log.warn("Embedding 缓存读取失败，删除后重建 key={}", cacheKey, ex);
            redis.delete(cacheKey);
        }
        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(List.of(text), null));
        float[] vector = response.getResult().getOutput();
        redis.opsForValue().set(cacheKey, toList(vector), properties.getEmbeddingCacheTtlHours(), TimeUnit.HOURS);
        return vector;
    }

    private List<Float> toList(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float value : vector) {
            values.add(value);
        }
        return values;
    }

    private String buildCacheKey(String text) {
        String model = properties.getEmbeddingModel();
        int hash = text.trim().hashCode();
        return "ai:embed:TEXT:" + hash + ":" + model;
    }

    public static double cosineSimilarity(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) {
            return 0;
        }
        double dot = 0;
        double normLeft = 0;
        double normRight = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            normLeft += left[i] * left[i];
            normRight += right[i] * right[i];
        }
        if (normLeft == 0 || normRight == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normLeft) * Math.sqrt(normRight));
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }

    private record ScoredItem<T>(T item, double score) {}
}
