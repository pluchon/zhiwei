# 校园设施报修系统第二阶段 — 前后端缺口与待补清单

> 本文档整理第二阶段**已实现前端但后端未对齐/未完成**的项，以及相对 `need-step2.md` §4.10 仍缺的测试与验收内容。
>
> 主需求仍以 `need-step2.md` 为准；实现规范遵守 `.codex/codex.md` 与 `.cursor/cursor.md`。
>
> 最后核对时间：2026-06-09（基于当前 `Campus-Backen` + `RuoYi-Vue3-master` 代码）。

## 1. 文档目的

第二阶段主体功能已落地，前端生产构建与后端 Maven 测试均可通过，但**不能视为 100% 严格验收**。本文件用于：

- 记录前端页面/接口调用所依赖、后端尚未补齐的行为；
- 记录需求明确要求、实现仍不完整的能力；
- 给出建议补全顺序与可核对的验收标准；
- 避免后续开发时重复排查同一缺口。

## 2. 现状摘要

| 维度 | 状态 |
|---|---|
| 后端 `mvn test` | 约 15 项用例通过 |
| 前端 `npm run build:prod` | 通过 |
| 核心业务主流程 | 位置结构化、派单、周期提醒、看板、通知已读优化等已可用 |
| 严格验收 | 存在下文列出的前后端对齐缺口与测试覆盖不足 |

## 3. 前端已依赖、后端未对齐（优先补）

以下项会直接导致页面展示异常、筛选无效或需求验收失败，建议作为 **P0** 处理。

### 3.1 工单附件预签名 URL（`signedUrl`）

**前端现状**

- `views/repair/detail.vue` 使用 `a.signedUrl` 渲染现场与维修图片。
- 无 `signedUrl` 时显示占位「OSS 图片」，用户无法预览私有桶图片。

**后端现状**

- `RepairAttachmentVO` 仅有 `objectKey`，无 `signedUrl` 字段。
- `OssService.signedUrl(String key)` 已实现，但 `RepairOrderServiceImpl.detail()` 转换附件时未调用。

**建议补全**

1. `RepairAttachmentVO` 增加 `signedUrl`（或等价的临时访问 URL 字段）。
2. 在 `detail()`（及必要时 `uploadAttachment` 返回值）中，对每个 `objectKey` 调用 `OssService.signedUrl` 填充。
3. OSS 不可用时按现有项目惯例返回明确错误或空值，前端继续走 `#error` 插槽。

**验收**

- 报修人/维修师傅/管理员打开含附件的工单详情，能正常预览图片（本地 MinIO/OSS 配置正确时）。

**关联文件**

- 后端：`RepairAttachmentVO.java`、`RepairOrderServiceImpl.java`、`OssService.java`
- 前端：`views/repair/detail.vue`

---

### 3.2 工单详情缺少维修师傅姓名/工号

**前端现状**

- 详情页在 `status >= 3` 时展示 `repairerRealName`、`repairerUserNo`（`detail.vue`）。
- 报修人接单后应能看到实际维修师傅信息（`need-step2-vue-code.md` §9）。

**后端现状**

- `enrichAdminListFields()` 仅在**管理员列表**分页时填充报修人/维修师傅姓名与工号、长时间未进展标记。
- `detail()` 只调用 `enrichOrderVo()`，后者仅处理联系方式脱敏与繁忙程度，**不填充** `repairerRealName` / `repairerUserNo`。

**建议补全**

- 抽取「按 userId 填充报修人/维修师傅展示字段」的共用逻辑；
- 在 `detail()` 中，对有权查看维修师傅信息的角色（报修人、当前维修师傅、管理员）填充上述字段；
- 接单前（`status < 3`）继续不暴露维修师傅信息。

**验收**

- 学生/教师查看已接单工单详情，能看到当前维修师傅姓名与工号；
- 待匹配/待接单工单详情不展示维修师傅信息。

**关联文件**

- 后端：`RepairOrderServiceImpl.java`（`detail`、`enrichAdminListFields`、`enrichOrderVo`）
- 前端：`views/repair/detail.vue`

---

### 3.3 站内通知类型未按第二阶段细分

**需求依据**

- `need-step2.md` §4.7：「在第二阶段通知类型增加后」支持按通知类型筛选。
- 前端通知页已提供类型筛选下拉（`views/notification/index`）。

**前端现状**

- `utils/repair.js` 中 `notificationTypes` 仅一项：`{ value: 0, label: "工单通知" }`。
- 筛选 UI 已就绪，但无法区分维修周期提醒、待确认提醒、派单、自动完成等。

**后端现状**

- `RepairOrderServiceImpl.notify()` 与 `RepairOrderTaskServiceImpl.notify()` 写入通知时**一律** `notificationType = 0`。
- `NotificationServiceImpl.list()` 已支持按 `notificationType` 过滤，但库中无细分数据。

**建议补全**

1. 在 `common/enums` 增加 `NotificationType`（或与需求一致的类型编码表），至少覆盖：
   - 工单状态/流转类（接单、派单、退回、完成等）；
   - 维修周期三天/七天提醒；
   - 待确认三天/七天/二十七天提醒；
   - 待确认三十天自动完成；
   - 长时间未进展管理员关注（七天提醒管理员）；
   - 自动完成仲裁申请（通知管理员，见 §3.4）。
2. 所有 `notify(...)` 调用点写入对应类型。
3. 前端 `notificationTypes` 与后端枚举保持一致。

**验收**

- 通知列表可按不同类型筛选且结果正确；
- 新产生的各业务通知带有正确 `notificationType`。

**关联文件**

- 后端：`RepairOrderServiceImpl.java`、`RepairOrderTaskServiceImpl.java`、新建枚举
- 前端：`utils/repair.js`、`views/notification/index.js`

---

### 3.4 自动完成仲裁申请未通知管理员

**需求依据**

- `need-step2.md` §4.5.3：申请成功后进入管理员仲裁流程。

**前端现状**

- 报修人可在自动完成七天内点击「申请仲裁」（`detail.vue` / `requestAutoCompleteArbitration`）。

**后端现状**

- `requestAutoCompleteArbitration()` 完成状态流转与系统评论，**未**向管理员发送站内通知。
- 对比：`RepairOrderTaskServiceImpl.notifyActiveAdmins()` 已在七天维修周期提醒中通知管理员，可复用类似模式。

**建议补全**

- 仲裁申请成功后，向所有启用管理员发送站内通知（含工单 ID、申请说明摘要）；
- 通知类型使用 §3.3 中的「仲裁申请」类型。

**验收**

- 报修人提交自动完成仲裁申请后，管理员在通知列表能看到对应提醒，点击可进入工单详情。

**关联文件**

- 后端：`RepairOrderServiceImpl.java`（`requestAutoCompleteArbitration`）

---

### 3.5 待接工单大厅忽略前端筛选参数

**前端现状**

- 维修师傅「待接工单」路由复用 `views/repair/list.vue`，与我的工单列表共用筛选表单（编号、标题、状态、校区、楼栋、时间等）。
- `list.js` 对待接列表调用 `listAvailableOrders(buildParams())`，会把上述参数一并传给后端。

**后端现状**

- `GET /repair/orders/available` 仅接收 `pageNum`、`pageSize`。
- `RepairOrderServiceImpl.available()` 固定查询：`PENDING_ACCEPT` + 当前师傅能力匹配的 `categoryId`，**忽略** `RepairOrderQueryDTO` 中其他条件。

**影响**

- 维修师傅在待接大厅使用筛选条件时，界面有筛选但结果不变，易造成误解。

**建议补全（二选一，需与产品确认）**

- **方案 A（推荐）**：待接大厅前端隐藏/禁用管理员专属与无意义筛选项，仅保留对大厅有意义的条件（如校区、楼栋、故障类型、标题关键词）；后端 `available()` 扩展接收并应用这些条件。
- **方案 B**：前端待接路由使用精简列表页，不传组合筛选参数；文档明确「待接大厅不支持组合筛选」。

**验收**

- 筛选行为与界面一致；或界面明确不提供无效筛选。

**关联文件**

- 后端：`RepairOrderController.java`、`RepairOrderServiceImpl.available()`
- 前端：`views/repair/list.js`、`router/index.js`

---

### 3.6 「长时间未进展 = 否」筛选未实现

**前端现状**

- 管理员列表提供「长时间未进展」下拉：是 / 否（`list.vue`）。
- `list.js` 仅在 `longStagnant === true` 时把参数传给后端；选「否」时**不传参**。

**后端现状**

- `buildSearchWrapper()` 仅在 `Boolean.TRUE.equals(query.getLongStagnant())` 时收窄为停滞工单 ID 集合；
- 无「排除长时间未进展」逻辑。

**建议补全**

- 若保留「否」选项：前后端同时支持 `longStagnant=false`（排除 `findLongStagnantOrderIds()` 中的工单，且仍限制在已接单/处理中口径内）；
- 若需求只需正向筛选：前端移除「否」选项，仅保留「是」。

**验收**

- 管理员选择「是」仅看到停滞标记工单；若保留「否」，则不应包含停滞工单。

## 4. 需求相关、实现仍薄弱项（P1）

### 4.1 可控制时间的测试基础设施

**需求依据**

- `need-step2.md` §4.10.3：时间相关测试必须使用可控制的时间来源。

**现状**

- 运行时使用 `BusinessClock` 接口，但测试环境仅有 `SystemBusinessClock`，无 `FixedBusinessClock` / 测试替身。
- `RepairOrderTaskServiceTest` 等用例通过 `minusDays(4)` 造数据，依赖真实当前时间，难以精确覆盖 3/7/27/30 天边界。

**建议**

- 测试 profile 提供可注入的固定时钟；定时任务与周期相关测试通过调整时钟断言，而非依赖「碰巧」的时间差。

---

### 4.2 详情接口返回 `records` 但前端未展示

**现状**

- `RepairOrderDetailVO` 含 `records`（维修结果记录），后端 `detail()` 正常返回。
- 前端 `detail.js` 的 `detail` 对象未接收 `records`，页面无「维修记录」区块。

**说明**

- 不影响主流程，但第一阶段起详情数据模型与 UI 不一致；若需展示维修结果历史，需补前端区块（可单独排期）。

---

### 4.3 `longStagnant` 查询参数绑定风险（低）

**现状**

- 看板跳转列表使用路由参数 `longStagnant: "true"`，列表页 `onMounted` 会转为布尔值，一般可用。
- 若其他入口直接以 GET 字符串 `"true"`/`"false"` 请求 `GET /repair/orders`，需确认 Spring 对 `RepairOrderQueryDTO.longStagnant` 的绑定是否稳定。

**建议**

- 在 Controller 或 DTO 上增加显式转换/校验，或在接口文档中约定只接受布尔查询参数。

## 5. 后端已具备、前端尚未接线（对照用）

以下不属于「后端未写完」，但会造成「功能看起来缺失」，补前端即可。

| 能力 | 后端接口 | 前端现状 |
|---|---|---|
| 管理员驳回工单 | `POST /admin/orders/{id}/reject` | 详情页仅有「关闭工单」，无「驳回」按钮 |
| 管理员仲裁处理 | `POST /admin/orders/{id}/arbitrate` | 待仲裁（status=6）工单无仲裁操作入口 |
| 通知类型文案扩展 | 类型枚举待后端定义 | `notificationTypes` 仅一项 |

## 6. 测试覆盖缺口（相对 need-step2 §4.10）

当前后端测试类（约 8 个）已覆盖：位置快照与唯一性、派单操作日志、维修周期停滞 ID、三天提醒幂等、繁忙等级枚举、看板九项计数、工单编辑校验、名称排序等。

**仍缺或明显不足：**

| 类别 | 需求条目 | 现状 |
|---|---|---|
| 并发 | 同一工单只能成功派单/接单一次 | 无并发测试 |
| 派单 | 能力不匹配派单、二次校验、权限 | 仅操作日志一条路径 |
| 维修周期 | 七天提醒管理员、周期结束移除停滞标记、反馈未解决重新计时 | 仅三天提醒幂等 + 停滞 ID 查询 |
| 待确认 | 3/7/27 天提醒、30 天自动完成幂等 | 无专项测试 |
| 草稿清理 | 满 30 天逻辑删除幂等 | 无专项测试 |
| 仲裁 | 七天期限内/外申请、管理员通知 | 无专项测试 |
| 权限 | 跨用户/跨角色访问拒绝 | 无系统化权限测试 |
| 状态机 | 非法状态流转拒绝 | 仅少量编辑场景 |
| 工单查询 | 各角色数据范围、快捷筛选、管理员关键词 | 无专项测试 |
| 第一阶段回归 | 提交、接单、退回、反馈未解决、评价 | 无回归套件 |
| 时间可控 | 所有定时/周期边界 | 见 §4.1 |

**前端（§4.10.4）**

- 无自动化测试框架；依赖 `build:prod` + 人工验证角色、筛选、二次确认与文案。

## 7. 建议实现顺序

```text
P0-1  详情附件 signedUrl（用户可见收益最大）
P0-2  详情填充维修师傅展示字段
P0-3  通知类型枚举 + 写入点改造 + 前端类型表同步
P0-4  自动完成仲裁申请通知管理员
P0-5  待接大厅筛选：前后端方案对齐后实现
P0-6  长时间未进展「否」筛选：实现或移除 UI

P1-1  固定 BusinessClock 测试基础设施
P1-2  并发派单/接单测试
P1-3  待确认 3/7/27/30 天与草稿清理测试
P1-4  权限与状态机拒绝测试
P1-5  第一阶段核心流程回归测试

P2    前端补管理员驳回/仲裁按钮；详情展示 records（若需要）
```

## 8. 单项验收核对表

完成某条缺口后，可用下表自检：

- [ ] 工单详情图片可预览（私有 OSS 环境）
- [ ] 报修人于已接单工单详情可见维修师傅姓名/工号
- [ ] 通知列表按类型筛选结果正确，且新业务通知类型正确入库
- [ ] 自动完成仲裁申请后管理员收到通知并可跳转详情
- [ ] 待接大厅筛选与结果一致（或 UI 已收敛为不支持）
- [ ] 管理员「长时间未进展」筛选与列表标记一致
- [ ] `mvn test` 包含并发/定时/权限/回归用例且通过
- [ ] `npm run build:prod` 通过
- [ ] 第一阶段核心报修闭环人工抽测无回归

## 9. 关联文档

| 文档 | 说明 |
|---|---|
| `need-step2.md` | 第二阶段业务需求与验收基线 |
| `need-step2-code.md` | 后端 Java 设计 |
| `need-step2-vue-code.md` | 前端 Vue 设计 |
| `need-step2-sql-code.md` | 数据库与迁移设计 |
| `.codex/codex.md` | 项目长期规范 |
| `.cursor/cursor.md` | Cursor 实现约定 |

---

> 补全上述缺口后，建议在本文件顶部更新「最后核对时间」，并将已完成条目标记到 §8 核对表或单独维护 changelog。
