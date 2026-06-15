# 校园设施报修与资产管理系统第五阶段数据库设计

> 本文档只描述第五阶段核心数据库变更。Flyway 迁移只向前新增，不修改历史迁移。

## 1. 迁移范围

建议新增迁移：

```text
V6__step5_ai_enhancement.sql
```

## 2. 扩展资产导入批次与卡片

`asset_import_batch` 新增：

```text
source_type VARCHAR(20) NOT NULL DEFAULT 'EXCEL'
```

- 仅使用 `EXCEL`、`IMAGE`。
- Excel 与图片分开建批次，同一批次不得混合来源。

`asset_import_item` 新增：

```text
enabled_date DATE NULL
asset_description VARCHAR(1000) NULL
source_image_object_key VARCHAR(500) NULL
ai_recognize_status VARCHAR(20) NULL
```

- `source_image_object_key` 仅图片批次使用；确认入库后复制/绑定为正式资产图片。
- `ai_recognize_status` 仅使用 `SUCCESS / FAILED / PENDING`，不存置信度。
- 已有 `asset_category_id` 存模糊匹配结果；校区、楼栋、位置字段仍由管理员填写。

## 3. 扩展 `repair_order`

新增：

```text
suspected_duplicate TINYINT NOT NULL DEFAULT 0
duplicate_reason VARCHAR(1000) NULL
```

索引：

```text
KEY idx_repair_order_suspected (suspected_duplicate, status, create_time)
```

- 提交时 AI 判定疑似重复则写 `suspected_duplicate = 1` 与 `duplicate_reason`。
- 标记仅用于筛选和展示，不是工单业务状态。

## 4. 新增 `repair_order_ai_link`

核心字段：

```text
link_id BIGINT AUTO_INCREMENT
source_order_id BIGINT
target_order_id BIGINT
link_type VARCHAR(20)
ai_reason VARCHAR(1000) NULL
confirmed TINYINT NOT NULL DEFAULT 0
operator_id BIGINT NULL
create_time DATETIME
update_time DATETIME
delete_state TINYINT
```

索引：

```text
PRIMARY KEY (link_id)
KEY idx_ai_link_source (source_order_id, confirmed, delete_state)
KEY idx_ai_link_target (target_order_id, delete_state)
UNIQUE KEY uk_ai_link_pair (source_order_id, target_order_id, delete_state)
```

- 仅疑似重复工单详情加载时写入推荐记录；管理员确认后 `confirmed = 1`。
- 解除关联使用逻辑删除；不影响工单状态字段。

## 5. 新增 `ai_audit_log`

核心字段：

```text
audit_id BIGINT AUTO_INCREMENT
operator_id BIGINT NULL
scene_type VARCHAR(50)
target_type VARCHAR(50) NULL
target_id BIGINT NULL
result_status VARCHAR(20)
failure_reason VARCHAR(500) NULL
create_time DATETIME
update_time DATETIME
delete_state TINYINT
```

索引：

```text
PRIMARY KEY (audit_id)
KEY idx_ai_audit_operator (operator_id, create_time)
KEY idx_ai_audit_scene (scene_type, create_time)
KEY idx_ai_audit_target (target_type, target_id)
```

- `scene_type` 覆盖资产识别、重复报修、关联推荐、派单分析、NL 统计、NL 导出。
- 禁止保存 prompt 全文、模型原文和 API Key。

## 6. 新增 AI 助手会话（可选落库）

若多轮会话需短期持久化，新增：

```text
ai_assistant_session
ai_assistant_message
```

最小字段：

```text
session_id, user_id, scene_type, expire_time
message_id, session_id, role, content_summary, create_time
```

- 会话仅当前登录周期有效，不跨会话长期保存完整聊天。
- `content_summary` 仅存摘要，不存完整敏感 prompt。

## 7. 核心条件更新

- 导入卡片确认仍仅 `PENDING` 可执行；图片批次确认时绑定 `source_image_object_key` 到正式资产。
- 工单关联确认/解除必须校验管理员权限与 `source_order_id` 归属。
- 重复报修候选查询必须过滤：同故障类型、7 天内、进行中状态、未逻辑删除。
- AI 审计日志只允许新增，不允许修改历史记录。

## 8. 数据迁移与兼容

- 历史导入批次默认 `source_type = EXCEL`。
- 历史工单默认 `suspected_duplicate = 0`。
- 不修改 V5 及以前迁移文件。
