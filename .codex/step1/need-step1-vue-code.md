# 校园设施报修与资产管理系统 Vue3 前端设计 - 第一阶段

> 一致性约定：第一阶段实现以 `need-step1-java-code.md` 为主；本文档只描述前端实现，不自行改变接口、状态或数据模型。

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
- Controller 只能声明接口注解、接收参数、调用 Service 和包装 `ApiResponse`，禁止直接依赖 Mapper、写业务逻辑、写数据库逻辑、创建业务实体、处理事务或写审计逻辑。
- Spring 管理的 Controller、Service、Component、Configuration、Filter 等类，依赖注入统一使用字段 `@Autowired`。
- Entity 到 VO 的转换必须统一放在 `converter` 包中，使用可直接维护的手写静态转换方法，禁止依赖 MapStruct 等编译期自动生成转换实现。
- ServiceImpl 必须直接调用 converter 静态方法，禁止将 converter 注册为 Spring Bean 或通过 `@Autowired` 注入。
- ServiceImpl 禁止编写业务载荷、Redis 载荷、DTO 或 VO 内部类；此类数据结构必须抽取为独立 Java 文件并补充中文 Javadoc。
- ServiceImpl 中所有事务方法必须显式使用 `@Transactional(rollbackFor = Exception.class)`。
- 重要类、重要方法和关键业务分支必须补充中文注释，说明业务原因、权限边界、事务边界和并发约束。
- 全部数据库主键必须使用 MySQL `BIGINT AUTO_INCREMENT` 自增策略，禁止雪花 ID、`assign_id` 或应用层手动生成主键。

## 1. 技术与定位

前端使用 RuoYi-Vue3 的布局、主题和通用组件，不依赖 RuoYi 后端。

第一阶段技术栈：

```text
Vue 3 Composition API
Vite
Pinia
Vue Router
Axios
Element Plus
Sass
Tianai 行为验证码交互
```

约束：

- 全局状态使用 Pinia。
- 页面访问经过 Vue Router 路由守卫。
- 所有请求通过统一 Axios 实例。
- 路由和菜单按固定角色静态维护，不请求 RuoYi 动态菜单接口。
- 前端权限只改善交互体验，后端是最终安全边界。

## 2. 目录规范

```text
src
├── api              按业务模块封装接口
├── assets
│   ├── icons/svg
│   ├── images
│   └── styles
├── components       跨业务通用组件
├── layout           RuoYi 页面布局
├── router           静态路由与路由守卫
├── store/modules    Pinia Store
├── utils            Axios、Token、校验和工具
└── views
    ├── auth
    ├── dashboard
    ├── repair
    ├── notification
    ├── profile
    └── admin
```

主要 API 模块：

```text
api/auth.js
api/repair/order.js
api/repair/category.js
api/notification.js
api/admin/user.js
api/admin/dict.js
api/admin/audit.js
```

主要页面：

```text
认证：登录、激活、账号恢复、手机号与邮箱换绑
学生/教师：提交报修、我的报修、工单详情
维修师傅：待接工单、我的工单、工单详情
管理员：工单、用户、故障类型、维修能力、字典、审计日志
通用：通知、个人中心、401、404
```

## 3. 状态与组件

Pinia Store：

```text
user          Token、当前用户、角色
permission    当前角色路由和菜单
app           布局状态
settings      主题设置
tagsView      页签状态
dict          字典缓存
notification  未读通知数量
```

- Token 统一通过 `utils/auth.js` 读写。
- 页面局部表单、列表和加载状态不放入 Pinia。
- 页面不能直接修改 Store 内部状态。
- 退出登录、401、账号停用或凭证变化时清除 Token、用户信息、路由和页面缓存。
- 密码、验证码及 `captchaTicket`、`verificationId` 等业务票据只保存在当前页面内存。

优先使用 Element Plus 和保留的 RuoYi 通用组件：

```text
表单：el-form、el-input、el-select
列表：el-table、el-pagination、el-tag
详情：el-card、el-descriptions、el-timeline、el-steps
反馈：ElMessage、ElMessageBox、ElNotification
弹层：el-dialog、el-drawer
上传：el-upload
```

每个异步页面必须处理：

```text
loading
empty
error
submitting
disabled
```

提交期间禁用按钮，后端仍负责幂等和并发控制。

## 4. 路由与权限

固定角色编码：

```text
STUDENT
TEACHER
REPAIRER
ADMIN
```

角色路由：

```text
公共：登录、激活、账号恢复、401、404
学生/教师：首页、提交报修、我的报修、通知、个人中心
维修师傅：首页、待接工单、我的工单、通知、个人中心
管理员：首页、工单、用户、故障类型、维修能力、字典、审计日志、通知、个人中心
```

路由守卫：

```text
检查 Token
→ 无 Token 且非白名单：跳转登录页
→ 有 Token 但未加载用户：请求当前用户
→ 根据数据库返回的当前角色生成可访问路由
→ 无角色权限：跳转 401
```

白名单：

```text
/login
/activation
/recovery
/401
/404
```

- 路由 `meta.roles` 控制页面访问。
- 按钮根据角色、资源归属和工单状态展示或禁用。
- 后端拒绝操作时，前端以服务端结果为准并刷新页面数据。

## 5. Axios 与 API

统一 Axios 实例负责：

- 设置 `baseURL`、超时时间和 `Authorization: Bearer <token>`。
- 解析统一 `ApiResponse<T>`。
- 处理 401、403、限流和业务错误。
- 展示用户可理解的错误信息并记录 `traceId`。

规则：

- 页面禁止直接调用 Axios。
- GET 参数使用 `params`，POST、PUT 使用 `data`。
- 文件上传使用 `FormData`。
- 401、`AUTH_CREDENTIAL_CHANGED`、`AUTH_ACCOUNT_DISABLED` 清除登录状态并返回登录页。
- `AUTH_ACCOUNT_NOT_ACTIVATED` 引导进入激活页。

认证接口：

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

## 6. Tianai 行为验证码

Tianai 行为验证码与业务 `captchaTicket` 分离：

```text
请求 /auth/captcha/challenge
→ 展示 Tianai 验证码
→ 用户完成验证
→ 将 Tianai 验证结果、业务场景和目标地址提交到 /auth/captcha/ticket
→ 后端校验成功后返回业务 captchaTicket
→ 密码登录时提交 captchaTicket 到 /auth/login/password
→ 发送短信或邮箱验证码时提交 captchaTicket 到 /auth/verification-codes
```

- 前端不自行判断 Tianai 验证是否可信，最终由后端校验。
- `captchaTicket`、`verificationId` 和激活/恢复票据只保存在当前流程页面内存。
- 验证码倒计时依据后端返回的 `retryAfter`。
- 页面离开时清理倒计时和流程票据。
- 票据过期或被消费后，重新开始对应验证步骤。

密码登录：

```text
获取并完成 Tianai 验证
→ 获取 LOGIN_PASSWORD captchaTicket
→ 提交 userNo、密码和 captchaTicket
→ 登录成功后保存 JWT
```

## 7. 认证页面行为

账号激活：

```text
提交 userNo 和初始密码
→ 获取 activationTicket
→ Tianai 行为验证
→ 发送并校验预留手机号验证码
→ 设置新密码
→ 激活成功后返回登录页
```

账号恢复：

```text
选择主手机号或验证邮箱
→ Tianai 行为验证
→ 校验验证码
→ 设置与当前密码不同的新密码
→ 成功后返回登录页
```

联系方式换绑：

- 按后端要求分别验证当前身份凭证和新联系方式。
- 换绑成功后清除前端登录状态并返回登录页。

## 8. 工单页面

- 工单状态统一使用枚举映射和 `RepairStatusTag`，页面禁止散落状态数字。
- 工单详情使用 `el-descriptions`。
- 流转日志使用 `el-timeline`。
- 状态流程使用 `el-steps`。
- 评论使用列表或时间线，不模拟聊天应用。
- 列表筛选变化后回到第一页。
- 关闭、驳回、退回、仲裁等关键操作必须二次确认。
- 工单创建使用前端生成的 `requestId`，重复提交沿用同一值。

图片规则：

```text
报修提交：1～5 张
维修结果：0～5 张
单张最大：20 MB
```

## 9. SVG 与样式

- 优先使用 `@element-plus/icons-vue` 和现有 RuoYi SVG。
- 新增 SVG 放入 `src/assets/icons/svg`。
- 文件名使用小写 `kebab-case`。
- SVG 优先使用 `currentColor`，不写死复杂颜色、阴影和文字。
- 页面不堆积行内样式，不重复实现 Element Plus 已提供的组件。

## 10. RuoYi 保留与删除

保留并改造：

```text
layout
Pinia 基础 Store
Vue Router 路由守卫
Axios request 封装
Element Plus
字典页面和组件
登录日志与操作日志页面
分页、图片上传、图片预览、工具栏
主题、侧边栏、页签和错误页
```

删除：

```text
代码生成器
表单构建器
Swagger 页面
定时任务
缓存监控
在线用户
服务监控
Druid 监控
部门管理
岗位管理
菜单管理
角色管理
参数配置
公告模块
原用户管理页面
自行注册页
RuoYi 宣传首页和文档链接
```

站内通知使用本项目 `user_notification` 接口；用户管理页面按照当前单角色模型重新实现。

## 11. 第一阶段 TODO List

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
