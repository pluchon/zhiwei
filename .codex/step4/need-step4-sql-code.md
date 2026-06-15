# 校园设施报修与资产管理系统第四阶段数据库设计

> 本文档只描述第四阶段核心数据库变更。Flyway 迁移只向前新增，不修改历史迁移。

## 1. 迁移范围

建议新增迁移：

```text
V5__step4_operation_enhancement.sql
```

## 2. 调整 `asset`

新增字段：

```text
purchase_date DATE NULL
```

已购入年数和月数由查询结果动态计算，不入库。

## 3. 新增资产导入批次与卡片

新增 `asset_import_batch`：

```text
batch_id BIGINT AUTO_INCREMENT
file_name VARCHAR(255)
operator_id BIGINT
total_count INT
pending_count INT
confirmed_count INT
ignored_count INT
create_time DATETIME
update_time DATETIME
delete_state TINYINT
```

关键约束与索引：

```text
PRIMARY KEY (batch_id)
KEY idx_import_batch_operator (operator_id, create_time)
KEY idx_import_batch_active (delete_state, create_time)
```

新增 `asset_import_item`：

```text
item_id BIGINT AUTO_INCREMENT
batch_id BIGINT
row_number INT
asset_name VARCHAR(150) NULL
category_text VARCHAR(100) NULL
purchase_date DATE NULL
location_text VARCHAR(500) NULL
status VARCHAR(20)
duplicate_hint VARCHAR(500) NULL
failure_reason VARCHAR(1000) NULL
confirmed_asset_id BIGINT NULL
create_time DATETIME
update_time DATETIME
delete_state TINYINT
```

关键索引：

```text
PRIMARY KEY (item_id)
KEY idx_import_item_batch_status (batch_id, status, delete_state)
KEY idx_import_item_confirmed_asset (confirmed_asset_id)
```

- 卡片状态仅使用 `PENDING / CONFIRMED / IGNORED`。
- `confirmed_asset_id` 仅确认成功后写入，用于防止重复入库并保留来源追踪。
- 批次统计数量随卡片状态变化在同一事务中更新。
- 删除批次使用逻辑删除，不删除 `confirmed_asset_id` 指向的正式资产。

## 4. 维修师傅接单状态

在维修师傅对应业务表新增：

```text
accepting_state VARCHAR(20)
pause_reason VARCHAR(500) NULL
expected_resume_time DATETIME NULL
```

关键索引：

```text
KEY idx_repairer_accepting (accepting_state, delete_state)
```

- 接单状态仅使用 `AVAILABLE / PAUSED`。
- 现有维修师傅默认迁移为 `AVAILABLE`。
- `PAUSED` 时暂停原因必填；恢复为 `AVAILABLE` 时清空暂停信息。

## 5. 新增 `manual_account_recovery`

核心字段：

```text
recovery_id BIGINT AUTO_INCREMENT
target_user_id BIGINT
new_phone VARCHAR(30)
status VARCHAR(20)
applicant_admin_id BIGINT
reviewer_admin_id BIGINT NULL
identity_check_note VARCHAR(1000)
review_note VARCHAR(1000) NULL
approved_time DATETIME NULL
expire_time DATETIME NULL
completed_time DATETIME NULL
create_time DATETIME
update_time DATETIME
delete_state TINYINT
active_target_user_id BIGINT GENERATED ALWAYS AS (
  CASE
    WHEN delete_state = 0 AND status IN ('PENDING', 'APPROVED')
    THEN target_user_id
    ELSE NULL
  END
) STORED
```

关键索引：

```text
PRIMARY KEY (recovery_id)
UNIQUE KEY uk_recovery_active_target (active_target_user_id)
KEY idx_recovery_target_status (target_user_id, status, delete_state)
KEY idx_recovery_expire (status, expire_time, delete_state)
KEY idx_recovery_applicant (applicant_admin_id, create_time)
KEY idx_recovery_reviewer (reviewer_admin_id, create_time)
```

- 状态仅使用 `PENDING / APPROVED / COMPLETED / REJECTED / EXPIRED`。
- `new_phone` 入库前标准化；完成换绑时继续依赖现有用户手机号唯一约束。
- 生成列唯一约束保证同一用户仅存在一条未结束申请。
- 待复核申请撤销使用逻辑删除并记录操作日志，不新增撤销状态。

## 6. 新增 `management_statistics_export_log`

核心字段：

```text
export_log_id BIGINT AUTO_INCREMENT
operator_id BIGINT
range_type VARCHAR(20)
result_status VARCHAR(20)
file_name VARCHAR(255) NULL
failure_reason VARCHAR(1000) NULL
create_time DATETIME
update_time DATETIME
delete_state TINYINT
```

索引：

```text
PRIMARY KEY (export_log_id)
KEY idx_statistics_export_operator (operator_id, create_time)
KEY idx_statistics_export_result (result_status, create_time)
```

统计本身使用现有工单、资产、维修历史和接单记录查询，不新增统计事实表。

## 7. 核心条件更新

- 导入卡片仅 `PENDING` 状态可编辑、忽略或确认。
- 卡片确认成功后原子更新为 `CONFIRMED` 并绑定正式资产 ID。
- 卡片忽略后原子更新为 `IGNORED`，禁止再次确认。
- 维修师傅接单与匹配必须校验接单状态为可接单。
- 人工恢复审批仅 `PENDING` 状态可执行，并校验审批人与发起人不同。
- 人工恢复仅 `APPROVED` 且 `expire_time` 未到期时可以完成手机号换绑。
- 人工恢复过期任务按状态与到期时间条件更新，保证幂等。

## 8. 数据迁移与兼容

- 新增字段为可空或提供明确默认值，保证历史资产和维修师傅数据可正常读取。
- Flyway 迁移只新增第四阶段表、字段和索引，不修改第三阶段历史迁移。
- 普通业务查询继续排除 `delete_state = 1` 数据。
