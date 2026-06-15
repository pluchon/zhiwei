# 校园设施报修与资产管理系统第五阶段 Java 代码设计

> 本文档只明确第五阶段 Java 核心实现，不展开普通 CRUD 和伪代码。
>
> 实现必须同时遵守 `.cursor/cursor.md` 与 `need-step5.md`。

## 1. 实现原则

- 严格遵守 `Controller -> Service -> Mapper`；请求使用 DTO，响应使用 VO。
- AI 只辅助，不直接写正式资产台账、不自动改工单状态。
- Spring AI + Spring AI Alibaba 统一封装 DashScope；API Key 读取 `${DASHSCOPE_API_KEY}`。
- AI 调用失败时降级：资产导入回退第四阶段规则识别，报修提交不因 AI 失败而阻断。
- AI 审计在事务成功后异步或同事务外记录，日志不含 prompt 全文和密钥。
- 微调仅覆盖资产识别、重复报修判断；其余场景使用基座模型 + Prompt/工具调用。

## 2. AI 底座

新增配置与封装：

```text
config/SpringAiAlibabaConfig
config/AiModelProperties
service.ai.AiClientGateway
service.ai.AiAuditService
service.ai.AiModelRouteService
```

- `AiClientGateway` 统一封装 chat、多模态、Embedding 与微调模型路由。
- 资产识别、重复报修优先走微调模型；不可用时降级基座模型。
- NL 查统计、NL 导出、派单分析、关联推荐走基座模型。
- `AiAuditService` 记录场景、操作人、目标 ID、结果状态，不记录敏感明文。

## 3. 资产导入 AI

扩展第四阶段导入模块：

```text
AssetImportController
AssetImportService / AssetImportServiceImpl
AssetAiRecognitionService
AssetCategoryFuzzyMatchService
```

核心扩展：

- 批次新增来源类型：`EXCEL`、`IMAGE`；Excel 与图片分开建批次。
- Excel 上传后先走第四阶段列名规则，再调用 AI 补全非标准列与缺失字段。
- 图片上传单次最多 10 张；每张生成一张待审核卡片并异步识别。
- AI 填充名称、分类、购入日期、启用日期、说明；分类经模糊匹配映射 `asset_category_id`。
- 校区、楼栋、位置描述不由 AI 预填；识别失败仍保留卡片与原图。
- 确认入库时原图写入正式资产图片；正式入库仍复用现有资产新增校验。

核心 DTO / VO：

```text
AssetImportImageUploadDTO
AssetImportAiRecognizeResultVO
```

## 4. 重复报修识别

扩展报修提交链路：

```text
RepairOrderSubmitService（或现有提交 Service 扩展）
DuplicateRepairDetectionService
DuplicateRepairEmbeddingService
```

核心流程：

```text
提交工单
→ 检索同故障类型、同位置或同资产、7 天内、进行中 Top3 候选
→ AI 判定是否疑似重复
→ 是：返回报修人概括提醒；提交成功后写 suspected_duplicate 标记与理由
→ 否：正常提交
```

- 进行中状态：`PENDING_DISPATCH / PENDING_ACCEPT / ACCEPTED / PROCESSING / PENDING_CONFIRM / PENDING_ARBITRATION`。
- 报修人侧只返回概括提醒，不返回候选工单明细。
- 提交事务内写工单标记；AI 判定失败不阻断提交。
- 重复报修场景使用微调模型或 Embedding 微调结果。

## 5. 工单关联与派单辅助

新增/扩展：

```text
RepairOrderAiController
RepairOrderLinkService
RepairOrderDispatchAnalysisService
```

- 仅 `suspected_duplicate = 1` 的工单，管理员打开详情时自动生成关联推荐。
- 关联推荐只读参考；管理员可确认或解除，不影响状态机。
- 待匹配、待接单列表与派单页提供「AI 分析」接口，仅管理员主动点击时生成文字分析。
- 派单分析不返回维修师傅名单，不自动派单。

核心 DTO / VO：

```text
RepairOrderLinkConfirmDTO
RepairOrderAiAnalysisVO
RepairOrderDuplicateDetailVO
```

## 6. 管理侧 AI 与统一助手

新增模块：

```text
AiAssistantController
AiAssistantSessionService
AiStatisticsQueryService
AiExportPreviewService
```

- 统一助手支持多轮对话，会话上下文仅当前登录会话有效。
- 管理员：NL 查全局统计、NL 导出预览（工单 + 统计）。
- 维修师傅：仅 NL 查本人统计；不可导出。
- 学生/教师无权限。
- NL 查统计通过工具调用现有 `ManagementStatisticsService`、`RepairerStatisticsService`，禁止绕过权限。
- NL 导出先解析筛选条件并返回预览 VO，管理员确认后调用现有导出 Service。

核心 DTO / VO：

```text
AiAssistantChatDTO
AiAssistantMessageVO
AiStatisticsQueryResultVO
AiExportPreviewVO
AiExportConfirmDTO
```

## 7. 与现有模块衔接

- 资产导入、工单提交、派单、导出、统计均复用前四阶段 Service 与校验。
- 图片 OSS 规则复用现有附件上传与权限校验。
- 不新增 AI 自动派单、关单、合并、直接入库逻辑。
- 不实现 AI 限流；不实现 PDF/Word 识别。

## 8. 核心测试

- 图片导入 10 张限制、识别失败保留卡片、确认后写入资产图片。
- Excel AI 增强后仍进待审核卡片，500 行限制仍生效。
- 重复报修：Top3、同故障类型、7 天、仅进行中、报修人概括提醒、管理员标记。
- 关联推荐：仅疑似重复详情触发生成；确认/解除不影响状态。
- 派单分析：仅点击触发、仅文字输出。
- NL 统计/导出：角色权限、多轮追问、先预览后确认。
- AI 审计：关键场景有日志、无敏感明文。
- 微调降级与基座降级回归。
