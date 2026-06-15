# 校园设施报修系统第二阶段数据库设计

> 本文档只描述第二阶段需求需要新增或调整的数据库内容。
>
> 第一阶段表结构继续以 `.codex/step1/need-step1-sql-code.md` 为基础，Flyway 迁移只向前新增，不修改历史迁移文件。

## 0. 数据库执行环境

- MySQL 使用项目根目录 `docker-compose.yml` 中的 `mysql:8.4` 容器。
- 宿主机连接地址为 `localhost:3307`，数据库名为 `campus_system`；实际账号密码继续读取现有环境变量和配置。
- 数据保存在 Docker 卷 `campus_mysql`，执行迁移或测试时不得随意删除数据卷。
- 第二阶段 SQL 通过 `Campus-Backen/src/main/resources/db/migration` 下的新 Flyway 迁移执行，不直接修改已执行的第一阶段迁移。
- Redis 同样由 Docker 提供，但第二阶段新增提醒、自动完成和草稿清理以数据库条件更新保证幂等，不依赖 Redis 保存业务事实。

## 1. 数据库变更范围

第二阶段数据库变更只服务以下需求：

- 校区、楼栋基础数据和工单位置快照。
- 管理员手动派单历史。
- 维修周期三天、七天提醒。
- 待确认三天、七天、二十七天提醒和三十天自动完成。
- 自动完成后七天仲裁期限。
- 长时间未进展筛选。
- 今日完成统计。
- 通知筛选和批量已读查询效率。

不新增资产、二维码、导出、实时通知、复杂统计或消息队列表。

新增 Flyway 迁移：

```text
V3__step2_repair_enhancement.sql
```

## 2. 新增 `campus`

用于管理员维护校区和报修人选择校区。

| 字段 | 类型 | 说明 |
|---|---|---|
| `campus_id` | `BIGINT AUTO_INCREMENT` | 主键 |
| `campus_name` | `VARCHAR(100)` | 校区展示名称 |
| `normalized_name` | `VARCHAR(100)` | 去除首尾空格并统一大小写后的唯一比较值 |
| `name_sort_key` | `VARCHAR(300)` | 拼音首字母排序键 |
| `description` | `VARCHAR(500)` | 校区说明，可为空 |
| `status` | `TINYINT` | 0 启用，1 停用 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |
| `delete_state` | `TINYINT` | 0 正常，1 已删除 |

约束与索引：

```text
UNIQUE KEY uk_campus_normalized_name (normalized_name)
KEY idx_campus_options (delete_state, status, name_sort_key, campus_name, campus_id)
```

唯一约束不包含 `delete_state`，保证逻辑删除后的校区继续占用名称。

## 3. 新增 `building`

用于管理员维护楼栋和报修人按校区选择楼栋。

| 字段 | 类型 | 说明 |
|---|---|---|
| `building_id` | `BIGINT AUTO_INCREMENT` | 主键 |
| `campus_id` | `BIGINT` | 所属校区 ID |
| `building_name` | `VARCHAR(100)` | 楼栋展示名称 |
| `normalized_name` | `VARCHAR(100)` | 去除首尾空格并统一大小写后的唯一比较值 |
| `name_sort_key` | `VARCHAR(300)` | 拼音首字母排序键 |
| `description` | `VARCHAR(500)` | 楼栋说明，可为空 |
| `status` | `TINYINT` | 0 启用，1 停用 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |
| `delete_state` | `TINYINT` | 0 正常，1 已删除 |

约束与索引：

```text
UNIQUE KEY uk_building_campus_name (campus_id, normalized_name)
KEY idx_building_options (campus_id, delete_state, status, name_sort_key, building_name, building_id)
```

同一校区的逻辑删除楼栋继续占用名称，不同校区允许同名楼栋。

## 4. 调整 `repair_order`

保留第一阶段已有 `campus` 和 `building` 字段，将其作为名称快照继续使用，避免破坏历史数据。

新增字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `campus_id` | `BIGINT` | 校区 ID；第一阶段历史工单可为空 |
| `campus_description_snapshot` | `VARCHAR(500)` | 报修时校区说明快照，可为空 |
| `building_id` | `BIGINT` | 楼栋 ID，可为空 |
| `building_description_snapshot` | `VARCHAR(500)` | 报修时楼栋说明快照，可为空 |
| `room` | `VARCHAR(100)` | 教室或房间，可为空 |
| `completion_time` | `DATETIME` | 工单真正完成时间，可为空 |
| `auto_completed_time` | `DATETIME` | 最近一次自动完成时间，可为空 |

规则：

- 第二阶段新工单必须保存 `campus_id`，历史工单不强制匹配校区 ID。
- `campus`、`building`、说明快照只在创建或允许编辑位置时更新。
- 基础位置改名、停用、删除或恢复不能更新历史工单快照。
- 自动完成后七天仲裁期限以 `auto_completed_time` 为起点。
- 今日完成看板使用 `completion_time`，不能使用普通 `update_time`。
- 所有真正进入已完成状态的动作写入 `completion_time`；只有三十天自动完成同时写入 `auto_completed_time`。
- 待匹配、待接单撤回为草稿和已驳回重新转草稿时，更新 `update_time`，作为草稿三十天清理的新起点。

新增索引：

```text
KEY idx_order_location_id (campus_id, building_id)
KEY idx_order_completion_time (completion_time, delete_state)
KEY idx_order_draft_cleanup (status, delete_state, update_time)
```

工单标题关键词使用普通模糊查询，不为第二阶段额外引入全文检索。

## 5. 调整 `repair_assignment`

第一阶段 `repair_assignment` 继续保存主动接单和退回历史，第二阶段增加管理员派单来源信息。

新增字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `assignment_source` | `TINYINT` | 0 主动接单，1 管理员手动派单 |
| `operator_id` | `BIGINT` | 执行手动派单的管理员 ID；主动接单为空 |
| `dispatch_note` | `VARCHAR(1000)` | 派单说明；主动接单为空 |
| `capability_mismatch_reason` | `VARCHAR(1000)` | 能力不匹配派单原因，可为空 |

规则：

- 每次主动接单或手动派单新增一条接单历史。
- 维修师傅退回只更新当前接单记录的状态和退回原因，不覆盖历史派单信息。
- 不新增管理员转派字段或转派记录。

## 6. 新增 `repair_work_cycle`

用于记录每次维修师傅连续处理周期和三天、七天提醒状态。

| 字段 | 类型 | 说明 |
|---|---|---|
| `work_cycle_id` | `BIGINT AUTO_INCREMENT` | 主键 |
| `order_id` | `BIGINT` | 工单 ID |
| `repairer_id` | `BIGINT` | 当前维修师傅 ID |
| `start_time` | `DATETIME` | 周期开始时间 |
| `end_time` | `DATETIME` | 周期结束时间，可为空 |
| `active_flag` | `TINYINT` | 活动周期为 1，结束后为空 |
| `three_day_reminded` | `TINYINT` | 三天提醒是否已发送 |
| `seven_day_reminded` | `TINYINT` | 七天提醒是否已发送 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |
| `delete_state` | `TINYINT` | 逻辑删除状态 |

索引：

```text
KEY idx_work_cycle_order (order_id, end_time)
KEY idx_work_cycle_three_day (end_time, three_day_reminded, start_time)
KEY idx_work_cycle_seven_day (end_time, seven_day_reminded, start_time)
UNIQUE KEY uk_work_cycle_active (order_id, active_flag)
```

规则：

- 活动周期使用 `active_flag = 1` 和 `end_time IS NULL` 表示，结束时将 `active_flag` 更新为空。
- 唯一约束保证同一工单只能存在一个活动维修周期；历史结束周期允许存在多条。
- 长时间未进展标记由“活动维修周期已满七天”计算，不在工单表重复保存标记字段。
- 长时间未进展查询同时要求工单仍处于已接单或处理中。
- 周期结束后自然不再属于长时间未进展。

## 7. 新增 `repair_confirmation_cycle`

用于记录每次待确认周期、提醒状态和自动完成状态。

| 字段 | 类型 | 说明 |
|---|---|---|
| `confirmation_cycle_id` | `BIGINT AUTO_INCREMENT` | 主键 |
| `order_id` | `BIGINT` | 工单 ID |
| `reporter_id` | `BIGINT` | 报修人 ID |
| `start_time` | `DATETIME` | 本次进入待确认的时间 |
| `end_time` | `DATETIME` | 本周期结束时间，可为空 |
| `active_flag` | `TINYINT` | 活动周期为 1，结束后为空 |
| `three_day_reminded` | `TINYINT` | 三天提醒是否已发送 |
| `seven_day_reminded` | `TINYINT` | 七天提醒是否已发送 |
| `twenty_seven_day_reminded` | `TINYINT` | 二十七天提醒是否已发送 |
| `auto_completed` | `TINYINT` | 本周期是否已自动完成 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |
| `delete_state` | `TINYINT` | 逻辑删除状态 |

索引：

```text
KEY idx_confirmation_order (order_id, end_time)
UNIQUE KEY uk_confirmation_active (order_id, active_flag)
KEY idx_confirmation_reminder (
    end_time,
    three_day_reminded,
    seven_day_reminded,
    twenty_seven_day_reminded,
    auto_completed,
    start_time
)
```

规则：

- 活动周期使用 `active_flag = 1` 和 `end_time IS NULL` 表示，结束时将 `active_flag` 更新为空。
- 唯一约束保证同一工单只能存在一个活动待确认周期；历史结束周期允许存在多条。
- 三十天自动完成使用周期状态和工单状态双重条件更新，防止重复完成。

## 8. 通知与查询索引调整

`user_notification` 增加组合索引：

```text
KEY idx_notification_filter (
    receiver_id,
    is_read,
    notification_type,
    create_time
)
```

用于当前用户按已读状态、通知类型分页查询，以及判断是否存在未读通知。

`sys_user` 增加：

```text
KEY idx_user_real_name (real_name)
```

用于管理员按报修人或当前维修师傅姓名查询工单；账号查询继续使用已有 `user_no` 唯一索引。

`sys_operation_log` 继续记录草稿自动清理结果，不新增任务日志表。

由于草稿自动清理由系统任务执行，`sys_operation_log.operator_id` 调整为可为空；为空表示系统任务，不伪造管理员用户 ID。

以下需求不新增字段或表：

- 维修师傅繁忙程度和整体维修力量根据现有工单状态实时聚合计算，不保存精确数量。
- 单条、批量和全部已读继续更新现有通知的 `is_read` 与 `read_time`。
- 未读提示小点通过判断是否存在未读通知实现，不保存未读数量。
- `Asia/Shanghai` 今日起止时间由后端计算后作为查询条件传入，数据库不新增时区字段。

## 9. 核心条件更新

第二阶段必须使用明确条件更新保护：

- 主动接单：仅待接单状态、版本一致时成功。
- 管理员手动派单：仅待匹配或待接单、版本一致时成功。
- 工单编辑：仅草稿、待匹配或待接单，且报修人和版本一致时成功。
- 撤回为草稿：仅待匹配或待接单、报修人和版本一致时成功。
- 已驳回转草稿：仅已驳回、报修人和版本一致时成功。
- 反馈未解决：一次更新内增加次数并决定进入处理中或待仲裁。
- 自动完成：仅活动待确认周期已满三十天且工单仍为待确认时成功。
- 草稿清理：仅草稿、未删除且最后更新时间已满三十天时成功。
- 各提醒标识：仅活动周期、尚未提醒且达到对应时间时成功。

条件更新影响行数不是 `1` 时，不继续写入后续记录。该规则用于并发保护和定时任务幂等。

## 10. 数据迁移与兼容

- 第一阶段工单已有 `campus` 和 `building` 文本继续作为历史名称快照。
- 第一阶段历史工单的新增位置 ID 和说明快照允许为空。
- 第一阶段接单记录迁移后默认 `assignment_source = 0`，表示主动接单。
- 第一阶段已完成工单若无法可靠确定完成时间，不使用 `update_time` 猜测回填；第二阶段今日完成统计只统计具有真实 `completion_time` 的记录。
- 不修改或删除第一阶段历史迁移文件。

## 11. 第二阶段不新增

- 不新增资产、学校、楼层、房间独立表。
- 不新增通知推送、消息队列或任务执行记录表。
- 不新增管理员转派记录。
- 不新增工单导出、趋势、排名或复杂统计表。
- 不物理删除工单、位置、周期、日志或通知数据。
