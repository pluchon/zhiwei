# 第五阶段 AI 模块最终收尾 Java 设计

> 本文档只明确 Java 核心实现。实现必须遵守 `.codex/codex.md` 与 `need-step5-gap.md`。

## 1. 通用要求

- 严格遵守 `Controller -> Service -> Mapper`。
- 请求、响应、Redis 载荷和结构化 AI 结果使用独立 DTO / VO。
- AI 结果必须经过后端权限、候选范围和字段合法性校验。
- AI 与 Embedding 失败时按需求降级，不阻断核心业务。
- 所有新增 AI 场景接入现有审计能力，禁止记录敏感明文。

## 2. Embedding 底座

新增统一 `AiEmbeddingGateway`：

- 基于 Spring AI Alibaba `EmbeddingModel`。
- 提供批量向量生成、余弦相似度排序、阈值判断和失败降级。
- Redis 按实体类型、实体 ID、更新时间和模型名缓存向量。
- 不引入专用向量数据库。

新增审计场景：

```text
ORDER_SEMANTIC_SEARCH
ASSET_SEMANTIC_SEARCH
SUGGESTION_SIMILARITY
ASSET_CATEGORY_MATCH
```

## 3. 重复报修与关联推荐

- `DuplicateRepairDetectionService` 在业务条件过滤后调用 Embedding 排序并选取 Top3。
- Embedding 失败时使用现有候选排序。
- `RepairOrderAiServiceImpl` 使用 `ORDER_LINK` 场景一次分析 Top3。
- AI 返回候选工单 ID、是否推荐和独立理由；后端只接受 Top3 范围内结果。
- AI 失败时保存 Top3 与统一降级理由。
- 使用持久化生成标记避免重复生成，以及解除全部关联后重新生成。

## 4. 派单分析与重复理由

- 保留现有派单分析接口，继续仅允许管理员访问。
- 派单分析只返回文字，不包含维修师傅推荐。
- 重复详情响应继续返回整体判定理由和逐条关联推荐理由。

## 5. 自然语言工单导出

- 扩展 AI 助手意图结果，使用独立 DTO 承接受控工单筛选条件。
- 后端校验状态、时间、布尔条件及基础数据唯一匹配后，再转换为 `RepairOrderQueryDTO`。
- 不支持或不合法条件直接返回明确提示，不得使用空查询条件。
- 预览阶段复用现有工单查询获取真实总数。
- Redis 预览载荷保存完整且已校验的查询条件；确认时复用现有导出 Service。
- 预计数量为 0 或超过 1000 时禁止确认导出。

## 6. 资产分类语义匹配

- 资产导入分类匹配优先使用 Embedding 对已启用分类排序。
- 低于阈值时留空；Embedding 失败时降级到现有字符串模糊匹配。
- 结果只写入待审核卡片。

## 7. 语义搜索与建议提醒

统一 AI 助手新增意图：

```text
ORDER_SEARCH
ASSET_SEARCH
SUGGESTION_SEARCH
```

- 历史工单搜索仅管理员可用，先按业务条件缩小候选，再进行语义排序。
- 资产搜索仅管理员和维修师傅可用，遵守现有资产可见权限。
- 建议搜索：管理员可搜索全部，维修师傅仅搜索本人。
- AI 助手响应新增工单、资产、建议结构化结果 VO。
- 结果数量设置合理上限，并提供详情所需业务 ID。

建议提交与重新提交前执行相似检测：

- 对本人相似建议返回标题、状态和 ID。
- 对他人相似建议仅返回存在相似内容的布尔提示。
- 检测失败不阻止提交。

## 8. 最小微调交付

新增 `.codex/step5/finetune/` 最小验收材料：

```text
README.md
dataset-spec.md
asset-recognition-sample.jsonl
duplicate-repair-sample.jsonl
acceptance-cases.md
acceptance-result-template.md
```

不实现训练平台、模型后台或自动训练流水线。

## 9. 核心测试

- Embedding 排序、缓存、阈值与失败降级。
- Top3 范围校验、空推荐、关联失败降级和不重复生成。
- 自然语言导出条件校验、真实数量、预览一致性和 1000 条限制。
- 工单、资产、建议语义搜索权限隔离。
- 建议相似提醒不泄露他人内容且不阻止提交。
- AI 审计不记录敏感明文。
