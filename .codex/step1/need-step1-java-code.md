# 校园设施报修与资产管理系统 Java 后端设计 - 第一阶段

## 代码格式强制规范

- 禁止将多个 `import`、字段、语句、方法、HTML 标签或 CSS 规则压缩在同一行。
- 每行只表达一个清晰操作；复杂条件、链式调用和长参数必须合理换行。
- 禁止为了减少文件行数牺牲代码可读性。
- Java 重要业务逻辑必须使用中文注释说明业务原因、事务边界和并发约束。
- Java 数据库实体类及字段必须使用中文 Javadoc 说明。
- Vue 文件只保留 `<template>` 与外部资源声明，禁止内嵌 JavaScript 和 CSS。
- 每个 Vue 文件必须使用同目录同名 `.js` 文件承载脚本；存在私有样式时，使用同目录同名 `.scss` 文件承载样式。
- Vue 通过 `<script setup src="./同名文件.js"></script>` 和 `<style scoped src="./同名文件.scss"></style>` 引入脚本与私有样式。
- 由于 Vue 编译器原生禁止 `<script setup>` 使用 `src`，项目必须保留 Vite 前置转换插件，在 Vue 编译前读取并注入同名脚本；禁止移除该插件后继续使用外部脚本声明。
- 没有私有样式时不创建空 `.scss` 文件；全局共享样式继续放在 `src/assets/styles`。
- 后端调用链必须严格遵守 `Controller -> Service -> Mapper`，禁止跨层调用。
- `service.interfaces` 只声明业务接口，`service.impl` 只放同名实现类，格式为 `XxxServiceImpl implements XxxService`。
- Controller 只能声明接口注解、接收参数、调用 Service 和包装 `ApiResponse`，禁止直接依赖 Mapper、写业务逻辑、写数据库逻辑、创建业务实体、处理事务或写审计逻辑。
- Spring 管理的 Controller、Service、Component、Configuration、Filter 等类，依赖注入统一使用字段 `@Autowired`。
- 请求体必须使用 DTO，接口响应必须使用 VO 或 `PageResult<VO>`，禁止 Controller 直接使用 `Map` 接收业务请求或直接返回数据库 Entity。
- Entity 到 VO 的转换必须统一放在 `converter` 包中，使用可直接维护的手写静态转换方法，禁止依赖 MapStruct 等编译期自动生成转换实现。
- ServiceImpl 必须直接调用 converter 静态方法，禁止将 converter 注册为 Spring Bean 或通过 `@Autowired` 注入。
- ServiceImpl 禁止编写业务载荷、Redis 载荷、DTO 或 VO 内部类；此类数据结构必须抽取为独立 Java 文件并补充中文 Javadoc。
- ServiceImpl 中所有事务方法必须显式使用 `@Transactional(rollbackFor = Exception.class)`。
- VO 只描述响应字段和中文字段说明，禁止在 VO 类中手写 `from`、`toVO` 等实体转换方法。
- 重要类、重要方法和关键业务分支必须补充中文注释，说明业务原因、权限边界、事务边界和并发约束。

## 1. 技术与分层规范

后端为独立 Spring Boot 单体应用，不使用 RuoYi 后端代码。前端使用 RuoYi-Vue3 外壳。

第一阶段技术栈：

```text
Java 17
Spring Boot 3
Spring Security
MyBatis-Plus
MySQL 8
Redis
JWT
BCrypt
Spring Validation
OSS SDK
tianai-captcha 1.4.1
```

Maven 依赖：

```xml
<dependency>
    <groupId>cloud.tianai.captcha</groupId>
    <artifactId>tianai-captcha</artifactId>
    <version>1.4.1</version>
</dependency>
```

代码目录：

```text
com.campus.system
├── common       通用响应、异常、枚举、工具和配置
├── controller   HTTP 接口
├── service      业务接口
│   └── impl     业务实现
├── mapper       数据库 CRUD 与必要查询
├── entity       数据库实体
├── dto          请求参数
└── vo           响应数据
```

分层约束：

- Controller 仅接收参数、执行参数校验、调用 Service、返回响应。
- Service 接口声明业务能力，ServiceImpl 实现业务流程、权限校验、事务和 Redis 操作。
- Mapper 仅负责数据库增删改查，不编写业务判断。
- DTO 接收请求，VO 控制响应，不直接向前端返回 Entity。
- Java 类型统一使用文件顶部 `import` 导入，业务代码禁止出现 `java.util.xxx` 等全限定类名。

## 2. 通用规范

统一响应：

```java
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    private String traceId;
}
```

统一异常类型：

```text
参数校验异常
认证异常
权限异常
资源不存在
业务状态冲突
请求限流
系统异常
```

- 业务错误码使用枚举维护。
- Filter 生成 `traceId`，写入 MDC 和响应。
- 所有输入使用 Spring Validation 校验。
- 手机号和邮箱入库前标准化。
- 日志禁止记录明文密码、验证码、JWT、业务票据、完整手机号和完整邮箱。

数据库规范：

```text
主键：BIGINT，统一使用 MySQL AUTO_INCREMENT 自增主键，禁止使用雪花 ID 或 assign_id
逻辑删除：delete_state，0 正常，1 已删除
通用字段：create_time、update_time、delete_state
固定状态：TINYINT + Java 枚举
```

- MyBatis-Plus 处理基础 CRUD、分页、逻辑删除和时间自动填充。
- 工单状态条件更新、接单并发等场景手写 SQL。
- 数据库事务内只处理必须保持一致的数据库数据。
- Redis 清理、短信、邮件等操作在数据库事务成功后执行。

## 3. 认证与登录

### 3.1 Redis Key

```text
auth:captcha-ticket:{captchaTicket}
value: scene、target、userId、businessTicket
TTL: 2 分钟

auth:verification:{verificationId}
value: scene、target、userId、codeDigest、errorCount
TTL: 5 分钟

auth:verification:cooldown:{scene}:{targetHash}
TTL: 60 秒

auth:verification:window:{scene}:{targetHash}
TTL: 15 分钟

auth:activation-ticket:{activationTicket}
TTL: 10 分钟

auth:recovery-ticket:{recoveryTicket}
TTL: 10 分钟

auth:login-failure:account:{userNoHash}
TTL: 30 分钟

auth:login-lock:account:{userNoHash}
TTL: 15 分钟

auth:login-failure:ip:{ipHash}
TTL: 30 分钟

login_tokens:{sessionId}
value: userId、securityStamp、expiresAt
TTL: 14 天
```

- Redis Key 不直接保存手机号和邮箱，使用服务端生成的摘要。
- 验证码摘要使用 `HMAC-SHA256(verificationId + code, serverSecret)`。
- 验证码最多校验 5 次，成功或达到上限后删除。
- 一次性票据必须原子消费；完成激活、恢复等数据库操作时采用“先占用，事务成功后删除”。

### 3.2 行为验证码与发送验证码

第一阶段使用 `tianai-captcha 1.4.1`，由后端生成并校验行为验证码挑战。

调用流程：

```text
请求 POST /auth/captcha/challenge
→ 后端返回 Tianai 行为验证码挑战
→ 前端展示并完成人机验证
→ POST /auth/captcha/ticket
→ 后端校验 Tianai 验证结果并签发业务 captchaTicket
→ 根据场景执行后续业务：
   - 密码登录：提交并消费 LOGIN_PASSWORD captchaTicket
   - 发送验证码：提交并消费对应场景 captchaTicket
```

规则：

- 业务票据通过请求体传递，不放入请求头。
- Tianai 挑战与业务 `captchaTicket` 职责分离，不能相互替代。
- `captchaTicket` 使用随机值作为 Redis Key，Value 绑定场景和目标地址。
- `captchaTicket` 既用于密码登录行为验证，也用于授权发送短信或邮箱验证码。
- 发送请求按场景分别计数。
- 同一目标地址 60 秒内只能发送一次，15 分钟内最多发送 5 次。
- 短信或邮件发送失败仍计入限流，不恢复票据。
- 找回账号等敏感入口返回模糊响应，避免枚举账号。

### 3.3 密码登录

```text
校验并消费 LOGIN_PASSWORD captchaTicket
→ 检查账号和 IP 登录失败限制
→ 根据 userNo 查询用户并校验 BCrypt 密码
→ 校验账号已激活且状态正常
→ 清除账号失败次数
→ 创建 Redis 会话并签发 JWT
```

- 密码连续错误 5 次后，禁止该账号使用密码登录 15 分钟。
- 账号和 IP 分别统计失败次数。
- 行为验证码失败不计入密码错误次数。
- 密码登录锁定不影响验证码登录、激活和账号恢复。

### 3.4 手机号验证码登录

```text
校验并消费 LOGIN_SMS verificationId
→ 从 Redis 获取可信手机号
→ 根据手机号查询用户
→ 校验账号已激活且状态正常
→ 创建 Redis 会话并签发 JWT
```

前端不能提交或指定用户 ID。

### 3.5 账号激活

```text
请求 POST /auth/activation/start
→ 校验 userNo、初始密码、未激活状态和正常账号状态
→ 签发 activationTicket，绑定 userId 和预留手机号
→ 获取并消费 captchaTicket，发送激活验证码
→ POST /auth/activation/complete
→ 占用 activationTicket，校验并占用 verificationId
→ 校验账号仍未激活、状态正常、新密码与初始密码不同
→ 开启数据库事务，更新密码、激活状态和 securityStamp
→ 事务成功后删除票据
→ 返回登录页
```

- 停用或封禁账号不能开始或完成激活。
- 重复完成激活返回“账号已激活”。
- 激活成功不创建登录会话、不签发 JWT。

### 3.6 账号恢复与修改密码

账号恢复：

```text
验证主手机号或验证邮箱
→ 签发 recoveryTicket
→ 占用 recoveryTicket
→ 校验账号已激活且状态正常
→ 校验新密码与当前密码不同
→ 事务内更新密码和 securityStamp
→ 事务成功后删除票据并清除全部 Redis 会话
→ 返回登录页
```

- 邮箱恢复后设置 `phone_confirm_required = 1`。
- 修改密码同样要求新密码与当前密码不同。
- 修改或恢复密码成功后不签发 JWT。

### 3.7 联系方式绑定与换绑

主手机号换绑：

```text
当前手机号可用：验证当前手机号 + 验证新手机号
当前手机号不可用：验证已绑定邮箱 + 验证新手机号
手机号和邮箱均不可用：普通用户由管理员特殊处理
```

验证邮箱：

```text
首次绑定：验证当前主手机号 + 验证新邮箱
更换邮箱：验证当前邮箱 + 验证新邮箱
当前邮箱不可用：验证主手机号 + 验证新邮箱
```

- 手机号和邮箱必须全系统唯一。
- 验证邮箱不允许直接解绑，只允许绑定或换绑。
- 两个验证结果分别签发短期票据，完成换绑时同时消费。
- 换绑事务内更新联系方式并重新生成 `securityStamp`。
- 事务成功后清除全部 Redis 会话，要求重新登录。
- 管理员特殊处理必须记录操作日志。

### 3.8 JWT 与鉴权

JWT 只保存 `sessionId` 和标准时间声明。

每次受保护请求：

```text
校验 JWT
→ 查询 Redis 会话
→ 根据 userId 查询数据库用户与角色
→ 实时校验激活状态、账号状态和 securityStamp
→ 写入 Spring SecurityContext
```

- 数据库是账号状态、角色和权限的事实来源。
- Redis 会话用于判断会话存在性、过期时间及保存登录时的 `securityStamp`。
- `securityStamp` 不一致时拒绝访问并删除当前会话。
- Controller 使用 `@PreAuthorize` 做角色级拦截。
- Service 校验资源归属、业务状态和具体操作权限。
- 未认证返回 401，无权限返回 403。
- 前端通过 `GET /users/me` 获取数据库实时校验后的当前用户和角色。

### 3.9 认证接口

```text
POST /auth/captcha/challenge
POST /auth/captcha/ticket
POST /auth/verification-codes
POST /auth/activation/start
POST /auth/activation/complete
POST /auth/login/password
POST /auth/login/phone
POST /auth/recovery/verify
POST /auth/recovery/complete
PUT  /auth/password
PUT  /auth/contacts/phone
PUT  /auth/contacts/email
POST /auth/logout
GET  /users/me
```

## 4. 用户与角色

固定角色编码：

```text
STUDENT
TEACHER
REPAIRER
ADMIN
```

管理员可以创建和维护学生、教师、维修师傅账号，调整这些普通角色，停用或封禁普通用户。

管理员不能创建管理员、提升普通用户为管理员或修改其他管理员。

停用、封禁、角色变更时：

```text
事务内更新用户数据和 securityStamp
→ 事务成功后清除全部 Redis 会话
→ 记录操作日志
```

## 5. 工单核心流程

### 5.1 草稿与提交

- 创建草稿时立即写入数据库，生成 `orderId`、`orderNo` 和 `requestId`。
- 同一用户重复提交相同 `requestId` 时返回已有工单。
- 草稿图片可为空；提交时必须有 1～5 张现场图片。
- 提交后根据故障类型能力关系动态匹配维修师傅：

```text
存在匹配维修师傅 → PENDING_ACCEPT
不存在匹配维修师傅 → PENDING_DISPATCH
```

### 5.2 动态匹配

第一阶段不保存候选维修师傅表。待接列表动态查询：

```text
工单状态为 PENDING_ACCEPT
+ 维修师傅账号正常
+ 维修师傅拥有该故障类型能力
```

- 接单时必须重新校验能力和账号状态。
- 新增能力后重新检查对应类型的 `PENDING_DISPATCH` 工单。
- 删除能力后，若某个 `PENDING_ACCEPT` 工单已无匹配人员，则改回 `PENDING_DISPATCH`。
- 维修师傅账号恢复正常或变为不可用时，同样重新检查受影响工单。

### 5.3 状态机

```text
PENDING_ACCEPT
→ ACCEPTED
→ PROCESSING
→ PENDING_CONFIRM
→ COMPLETED

提交与异常分支：
DRAFT → PENDING_ACCEPT：存在匹配维修师傅
DRAFT → PENDING_DISPATCH：不存在匹配维修师傅
PENDING_DISPATCH → PENDING_ACCEPT：后续存在匹配维修师傅
PENDING_DISPATCH / PENDING_ACCEPT → DRAFT
PENDING_DISPATCH / PENDING_ACCEPT → REJECTED
REJECTED → DRAFT
ACCEPTED → PENDING_DISPATCH
PENDING_CONFIRM → PROCESSING
第 5 次反馈未解决 → PENDING_ARBITRATION
管理员可将异常工单关闭为 CLOSED
```

所有状态变更必须通过明确的 Service 方法完成，禁止普通 CRUD 接口直接修改状态。

### 5.4 接单并发

接单事务：

```text
重新校验维修能力
→ 使用 status + version 条件更新工单
→ 插入 repair_assignment
→ 插入工单日志
→ 生成站内通知
```

条件更新行数为 0 时，返回“工单已被接走或状态已变化”。

`repair_assignment.status`：

```text
ACCEPTED
RETURNED
COMPLETED
CLOSED
```

### 5.5 处理与结果

- 只有当前维修师傅可以从 `ACCEPTED` 开始处理。
- 提交维修结果后进入 `PENDING_CONFIRM`。
- 维修结果说明和图片均可为空，图片最多 5 张。
- `attempt_no = unresolved_count + 1`，使用唯一约束防止重复提交。
- 第 1～4 次反馈未解决后返回 `PROCESSING`。
- 第 5 次反馈未解决后进入 `PENDING_ARBITRATION`。

### 5.6 完成与评价

- 只有报修人可以确认完成。
- 一个工单只能首次评价一次，并可追加一次追评。
- 评价对象为最终处理该工单的维修师傅。
- 使用 `order_id` 唯一约束防止重复评价。

## 6. 评论、附件与通知

评论：

- 报修人和当前维修师傅可以发表评论。
- 评论发送后 12 小时内允许本人撤回。
- 撤回后隐藏正文，保留发送人与时间，并记录操作日志。
- 管理员删除评论使用逻辑删除并记录操作日志。
- 系统评论不可由普通用户撤回或删除。

附件：

- 第一阶段由后端接收图片并上传 OSS。
- 单张图片最大 20 MB，仅允许后端可识别的常见图片格式。
- 数据库存储 `objectKey`，访问时生成临时签名 URL。
- 报修现场图片直接关联已创建的草稿工单。
- 维修结果图片在创建维修记录后关联对应 `record_id`。
- 上传和写入附件记录前校验当前用户对工单的操作权限。

通知：

- 工单关键状态变化生成站内通知。
- 站内通知与工单数据库变更处于同一事务。
- 短信和邮件发送失败不回滚数据库事务，只记录失败日志。

## 7. 基础数据与管理

- 故障类型被引用后只能停用，不物理删除。
- 维修能力关系使用逻辑删除；重新添加时恢复旧记录。
- 字典类型包括退回、驳回和关闭原因。
- `OTHER` 表示其他原因，选择后说明必填。
- 业务记录保存最终原因文本，不保存字典数据 ID。
- 管理员驳回、关闭、仲裁时，状态更新、工单日志、系统评论和站内通知处于同一事务。

## 8. 开发环境默认数据

第一阶段默认数据分为两类：

- Flyway 迁移 `V2__seed_base_data.sql` 初始化固定角色、原因字典和故障类型。
- `DevelopmentDataInitializer` 在应用启动时补齐开发演示账号和维修师傅能力关系。

### 8.1 固定角色

| 角色 ID | 角色编码 | 说明 | 状态 |
|---|---|---|---|
| `1` | `STUDENT` | 学生 | 启用 |
| `2` | `TEACHER` | 教师 | 启用 |
| `3` | `REPAIRER` | 维修师傅 | 启用 |
| `4` | `ADMIN` | 管理员 | 启用 |

### 8.2 开发演示账号

所有开发演示账号的统一密码为 `Campus123!`，账号均为已激活、状态正常、无需手机二次确认。

| 角色 | 账号 | 姓名/昵称 | 手机号 |
|---|---|---|---|
| 管理员 | `admin` | 系统管理员 | `13800000001` |
| 学生 | `student` | 演示学生 | `13800000002` |
| 教师 | `teacher` | 演示教师 | `13800000003` |
| 维修师傅 | `repairer` | 演示维修师傅 | `13800000004` |

演示账号仅用于本地开发和第一阶段联调，生产环境不得初始化默认管理员密码。

### 8.3 默认故障类型

| 类型 ID | 类型名称 | 说明 | 状态 |
|---|---|---|---|
| `1` | 水电维修 | 照明、供水与供电故障 | 启用 |
| `2` | 门窗维修 | 门锁、门窗及玻璃故障 | 启用 |
| `3` | 网络故障 | 校园网络与信息点故障 | 启用 |

演示维修师傅 `repairer` 默认具备以上三类维修能力，用于直接测试动态匹配和抢单流程。

### 8.4 默认原因字典

| 字典类型 | 字典名称 | 默认值 |
|---|---|---|
| `repair_return_reason` | 维修退回原因 | `CAPABILITY_MISMATCH` 能力不匹配、`OTHER` 其他 |
| `repair_reject_reason` | 报修驳回原因 | `INVALID` 无效报修、`OTHER` 其他 |
| `repair_close_reason` | 工单关闭原因 | `DUPLICATE` 重复且无需处理、`OTHER` 其他 |

### 8.5 默认空业务数据

系统默认不预置工单、接单记录、维修记录、附件、评论、评价、站内通知、登录日志和操作日志。
这些数据必须通过真实接口或联调流程产生。

## 9. 审计与测试

必须记录：

```text
登录成功与失败
密码修改与恢复
联系方式换绑和管理员特殊处理
用户状态、角色和维修能力调整
工单驳回、关闭和仲裁
评论删除
```

重点测试：

```text
验证码一次性消费、错误次数和限流
激活、恢复、换绑票据并发
JWT、Redis 会话和数据库实时鉴权
工单状态机非法流转
多人同时接单
重复提交工单、维修结果和评价
第 5 次反馈未解决进入仲裁
角色权限与资源归属
```

## 10. 第一阶段 TODO List

以下内容不在当前阶段实现：

- 超级管理员及管理员无可用验证方式时的手机号换绑。
- 管理处人工恢复账号、更换联系方式的审核材料与审批流。
- 多学校接入和学校级数据隔离。
- 学校统一身份认证。
- 学生移动端、小程序和 APP。
- 报修广场及公开报修信息。
- AI 重复报修识别、工单关联和维修师傅推荐。
- 复杂资产分析。
- 维修师傅提交故障类型建议。
- 视频上传。
- 草稿、撤回工单和未使用 OSS 文件自动清理。
- 复杂通知偏好、实时推送、WebSocket、SSE 和消息队列补偿。
- 待确认工单超时自动完成。
- Zipkin 等分布式链路追踪。
## 后端分层、Spring 注入与 DTO/VO 强制规范

- 后端调用链必须严格遵守 `Controller -> Service -> Mapper`，禁止跨层调用。
- `service` 包只允许放业务接口；同名实现类必须放在 `service.impl` 包中，并使用 `XxxServiceImpl implements XxxService`。
- Controller 禁止直接依赖 Mapper，禁止写业务逻辑、数据库逻辑、事务逻辑、审计逻辑和实体组装逻辑。
- Controller 仅允许声明接口注解、接收 DTO 参数、调用 Service、返回 `ApiResponse`。
- Spring Bean 依赖统一使用字段 `@Autowired` 注入。
- 所有请求体必须使用 DTO，禁止使用 `Map<String, Object>` 或 `Map<String, String>` 直接接收业务请求。
- 所有接口响应必须使用 VO 或 `PageResult<VO>`，禁止直接返回数据库 Entity。
- DTO 只表达请求参数，VO 只表达前端需要的响应字段；敏感字段如密码摘要、`securityStamp` 禁止出现在 VO。
- 重要类、重要方法和关键业务分支必须补充中文注释，说明业务原因、权限边界、事务边界和并发约束。
