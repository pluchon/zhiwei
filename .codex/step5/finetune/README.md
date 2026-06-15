# Step5 最小微调验收材料

本目录仅提供格式规范、虚构样例、最小训练发布步骤、验收用例和结果记录模板。

## 范围

- 资产识别（ASSET_RECOGNITION）
- 重复报修判断（DUPLICATE_REPAIR）

## 不包含

- 真实训练数据、密钥、敏感业务数据
- 训练平台、模型管理后台、自动训练流水线

## 使用方式

1. 阅读 `dataset-spec.md` 了解数据格式。
2. 参考 `asset-recognition-sample.jsonl` 与 `duplicate-repair-sample.jsonl` 构造本地训练集（勿提交真实数据）。
3. 按 README 中步骤在 DashScope 控制台完成最小微调与发布。
4. 使用 `acceptance-cases.md` 执行验收，并填写 `acceptance-result-template.md`。

## 降级要求

- 专用模型不可用时，系统应自动降级到基座模型。
- 降级不得阻断报修提交、资产审核或工单状态流转。
