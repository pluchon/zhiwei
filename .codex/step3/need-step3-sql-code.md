# 校园设施报修与资产管理系统第三阶段数据库设计

> 本文档只描述第三阶段核心数据库变更。Flyway 迁移只向前新增，不修改历史迁移。

## 1. 迁移范围

建议新增迁移：

```text
V4__step3_asset_and_repair_enhancement.sql
```

## 2. 新增 `asset_category`

核心字段：

```text
asset_category_id BIGINT AUTO_INCREMENT
category_name VARCHAR(100)
normalized_name VARCHAR(100)
status TINYINT
create_time DATETIME
update_time DATETIME
delete_state TINYINT
```

关键约束：

```text
UNIQUE KEY uk_asset_category_name (normalized_name)
KEY idx_asset_category_status (delete_state, status)
```

分类第三阶段不提供删除和恢复；存在未删除资产时禁止停用。

## 3. 新增 `asset`

核心字段：

```text
asset_id BIGINT AUTO_INCREMENT
asset_no VARCHAR(30)
asset_name VARCHAR(150)
asset_category_id BIGINT
campus_id BIGINT
building_id BIGINT NULL
floor VARCHAR(50) NULL
room VARCHAR(100) NULL
location_detail VARCHAR(500) NULL
status VARCHAR(30)
description VARCHAR(1000) NULL
enabled_date DATE NULL
image_object_key VARCHAR(500) NULL
active_order_id BIGINT NULL
version INT
create_time DATETIME
update_time DATETIME
delete_state TINYINT
```

关键约束与索引：

```text
UNIQUE KEY uk_asset_no (asset_no)
KEY idx_asset_query (delete_state, status, asset_category_id, campus_id, building_id)
KEY idx_asset_name (asset_name)
KEY idx_asset_active_order (active_order_id)
```

`active_order_id` 保存当前未结束关联工单；提交时原子占用，完成、驳回或关闭时释放。

## 4. 新增 `asset_status_log`

记录资产自动状态联动和管理员手动状态变化，只允许新增。

核心字段：

```text
asset_status_log_id BIGINT AUTO_INCREMENT
asset_id BIGINT
before_status VARCHAR(30)
after_status VARCHAR(30)
change_source VARCHAR(30)
related_order_id BIGINT NULL
operator_id BIGINT NULL
change_reason VARCHAR(500) NULL
create_time DATETIME
```

索引：

```text
KEY idx_asset_status_log (asset_id, create_time)
```

## 5. 调整 `repair_order`

新增资产关联与快照字段：

```text
repair_type VARCHAR(20)
asset_id BIGINT NULL
asset_no_snapshot VARCHAR(30) NULL
asset_name_snapshot VARCHAR(150) NULL
asset_category_snapshot VARCHAR(100) NULL
asset_location_snapshot VARCHAR(1000) NULL
exported_flag TINYINT
first_export_time DATETIME NULL
```

关键索引：

```text
KEY idx_order_asset (asset_id, status, delete_state)
KEY idx_order_exported (exported_flag, delete_state)
```

同一资产仅允许一个未结束工单，由事务内条件更新和工单状态校验共同保护。

## 6. 新增 `repair_order_export_log`

记录每次管理员导出：

```text
export_log_id BIGINT AUTO_INCREMENT
operator_id BIGINT
filter_snapshot TEXT
export_count INT
result_status VARCHAR(20)
file_name VARCHAR(255) NULL
failure_reason VARCHAR(1000) NULL
create_time DATETIME
update_time DATETIME
delete_state TINYINT
```

索引：

```text
KEY idx_export_operator_time (operator_id, create_time)
```

## 7. 新增 `repairer_suggestion`

核心字段：

```text
suggestion_id BIGINT AUTO_INCREMENT
repairer_id BIGINT
category VARCHAR(30)
title VARCHAR(200)
content VARCHAR(2000)
status VARCHAR(20)
withdrawn_flag TINYINT
admin_reply VARCHAR(2000) NULL
handler_id BIGINT NULL
handled_time DATETIME NULL
create_time DATETIME
update_time DATETIME
delete_state TINYINT
```

索引：

```text
KEY idx_suggestion_repairer (repairer_id, status, create_time)
KEY idx_suggestion_admin (status, category, create_time)
```

撤回后设置 `withdrawn_flag = 1`，不进入管理员待处理列表；重新提交后恢复为 `0`。已采纳和未采纳不可修改。

## 8. 附件与通知调整

- 现有附件记录补充或复用业务关联、草稿删除时间、OSS 删除状态、重试次数和最近失败原因。
- 为待清理附件建立到期扫描索引。
- 现有通知类型扩展系统通知和维修师傅建议相关通知。
- SSE 不新增业务事实表，站内通知表仍是事实来源。

## 9. 核心条件更新

- 资产修改、删除、停用：仅无未结束关联工单且版本一致时成功。
- 资产恢复：编号唯一、分类和位置可用时成功。
- 资产报修提交：仅资产使用中、未删除且 `active_order_id` 为空时原子占用成功。
- 工单接单或派单：同时将关联资产由使用中更新为维修中。
- 工单完成、驳回或关闭：释放对应资产的 `active_order_id`，并在允许条件下恢复为使用中。
- 导出成功：文件生成后幂等更新 `exported_flag`。
- 附件清理：删除前再次确认没有有效业务引用。
