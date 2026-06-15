# 微调数据集格式规范

## 通用

- 文件格式：JSON Lines（`.jsonl`）
- 每行一条样本，UTF-8 编码
- 不得包含真实手机号、邮箱、账号、密钥或业务敏感字段

## 资产识别（文本/结构化字段）

```json
{"messages":[{"role":"system","content":"你是校园资产识别助手，仅返回 JSON。"},{"role":"user","content":"资产名称：教室投影仪\n说明：第三教学楼 302"},{"role":"assistant","content":"{\"assetName\":\"教室投影仪\",\"categoryText\":\"教学设备\",\"locationText\":\"第三教学楼 302\"}"}]}
```

## 重复报修判断

```json
{"messages":[{"role":"system","content":"你是重复报修判定助手，仅返回 JSON。"},{"role":"user","content":"当前工单：标题=灯不亮，描述=302教室顶灯闪烁\n候选1：标题=302灯坏了，描述=同一教室顶灯不亮"},{"role":"assistant","content":"{\"suspected\":true,\"reason\":\"同位置同类故障，疑似重复报修\"}"}]}
```

## 样本量建议

- 每个场景 20~50 条虚构样本即可用于最小验收
- 优先覆盖：正常识别、无法判断、边界描述、否定样本
