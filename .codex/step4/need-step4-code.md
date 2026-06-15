# 校园设施报修与资产管理系统第四阶段 Java 代码设计

> 本文档只明确第四阶段 Java 核心实现，不展开普通 CRUD 和伪代码。

## 1. 实现原则

- 严格遵守 `Controller -> Service -> Mapper`，请求使用 DTO，响应使用 VO。
- 权限、状态、事务、并发和审计由 ServiceImpl 负责。
- Entity、DTO、VO 独立建文件，Entity 到 VO 使用手写静态 Converter。
- Excel 识别结果必须经过管理员确认后才能写入正式资产台账。
- 会话清理、短信、邮件和 SSE 在事务成功后执行，失败不得回滚核心业务。
- 状态修改使用当前状态条件更新，避免重复确认、重复审批和并发覆盖。

## 2. 资产购入信息与批量导入

扩展资产模块：

- 资产新增可空购入日期；VO 动态计算已购入年数和月数。
- 资产新增和修改继续复用现有权限、位置、分类和状态校验。
- 不保存已购入时长，不实现折旧、寿命和老化提醒。

新增导入模块：

```text
AssetImportController / Service / Mapper
AssetImportConverter
AssetExcelRecognitionService
```

核心 DTO / VO：

```text
AssetImportUploadDTO
AssetImportItemUpdateDTO
AssetImportConfirmDTO
AssetImportBatchVO
AssetImportItemVO
AssetImportConfirmResultVO
```

- 仅管理员上传 `.xlsx`，单次最多识别 500 行，空白行忽略。
- 自动识别资产名称、资产分类、购入日期和位置文本。
- 每次上传创建导入批次与待审核卡片，疑似重复只提示。
- 卡片仅待审核状态可编辑、忽略或确认。
- 卡片编辑只修改待审核数据，不直接修改正式资产。
- 批量确认逐卡事务处理，允许部分成功；失败卡片保留原因。
- 卡片确认时重新校验分类、位置、必填字段和当前状态。
- 正式入库复用现有资产新增规则与资产编号生成逻辑。
- 删除批次只逻辑删除批次及未入库卡片，不删除已确认资产。
- 上传、编辑、忽略、确认和删除批次记录操作日志。

## 3. 维修师傅接单状态

扩展维修师傅业务：

```text
RepairerAvailabilityService
RepairerAvailabilityUpdateDTO
RepairerAvailabilityVO
```

- 维修师傅本人设置可接单或暂停接单；暂停原因必填，预计恢复时间可空。
- 恢复接单时清空暂停原因和预计恢复时间。
- 管理员只允许查看，不允许修改。
- 暂停接单的维修师傅不参与动态匹配，也不能查看或接取抢单大厅新工单。
- 暂停接单不影响已经接手的工单，不自动恢复。
- 匹配、待接列表和接单操作均需重新校验接单状态。
- 状态变化记录操作日志，但不产生排班、请假或考勤记录。

## 4. 账号人工恢复

新增独立恢复申请模块：

```text
ManualAccountRecoveryController / Service / Mapper
ManualAccountRecoveryConverter
ManualAccountRecoveryExpireTask
```

核心 DTO / VO：

```text
ManualRecoveryCreateDTO
ManualRecoveryReviewDTO
ManualRecoveryPhoneVerifyDTO
ManualRecoveryQueryDTO
ManualRecoveryVO
```

- 仅管理员为学生、教师和维修师傅创建申请；管理员账号不适用。
- 创建前校验目标账号存在、角色适用且没有其他未结束申请。
- 发起管理员与审批管理员不得相同。
- 状态为 `PENDING / APPROVED / COMPLETED / REJECTED / EXPIRED`。
- 仅待复核申请可撤销或审批；仅已通过且未过期申请可完成验证。
- 审批通过后，新手机号验证有效期为三天。
- 验证码沿用现有验证码服务，并绑定人工恢复场景、申请和新手机号。
- 验证成功时再次校验手机号唯一性，原子更新手机号、`securityStamp` 和申请状态。
- 数据库事务成功后清除目标用户全部会话。
- 过期任务只将到期的已通过申请更新为已过期，单条失败不影响整批。
- 创建、撤销、审批、验证和完成均记录安全日志。

## 5. 管理统计与导出

扩展统计模块：

```text
ManagementStatisticsController / Service / Mapper
ManagementStatisticsExportService
RepairerStatisticsService
```

- 管理员查看维修效率、资产维修和全部维修师傅客观工作统计。
- 维修师傅仅查看本人统计；学生和教师无权限。
- 支持最近 7、30、90 天和本年度，默认 30 天。
- 统计范围统一转换为 `Asia/Shanghai` 起止时间。
- 接单数按接单时间、完成数与完成时长按最终完成时间、资产报修数按提交时间统计。
- 当前处理中数量与当前状态数量按实时数据统计。
- 平均时长由数据库聚合或专用统计 Mapper 查询，不在 Controller 计算。
- 统计导出同步生成 `.xlsx`，仅包含汇总数据，并记录导出日志。
- 导出失败只记录失败结果，不生成不完整文件。
- 不实现排名、绩效评分、风险评分和自动处置。

## 6. 通知与兼容

- 继续使用现有站内信、SSE、短信和邮件能力，不新增通知渠道。
- 通知发送失败不得回滚资产导入、人工恢复或其他核心事务。
- 新增查询和状态规则不得破坏现有资产报修、工单退回和抢单流程。

## 7. 核心测试

- 资产导入 500 行限制、列识别、卡片状态、重复确认、部分成功和权限。
- 暂停接单对匹配、抢单大厅、并发接单和已有工单的影响。
- 人工恢复双管理员限制、状态流转、手机号唯一性、三天过期和会话清理。
- 统计权限、时间口径、敏感信息保护和导出日志。
- 现有资产、工单、通知和认证流程回归。
