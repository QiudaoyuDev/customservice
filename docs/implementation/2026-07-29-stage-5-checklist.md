# 阶段 5：不可变故障流程版本与受控状态机

## 已实现

- 发布时将流程与节点序列化为 `troubleshoot_flow_version_snapshots`，新会话只读取该快照；活动会话以 `conversation_flow_sessions.flow_version_id` 固定版本。
- `conversation_flow_steps` 记录节点、受控/归一化回复、原始消息与流转结果，便于交接和审计。
- 流程匹配由 `FlowMatcher` 使用服务端会话产品上下文，涵盖 tenant、型号、变体、地区、语言、硬件 revision、固件范围、意图、可选触发词/错误码及优先级。
- 管理端仅允许编辑 DRAFT；`troubleshoot_flow_definitions` 提供稳定业务标识，发布版本需在同一定义下 clone 出新 DRAFT。发布前执行分支、可达性、循环、终止节点校验。
- 客户端按钮会传 `controlledReply`；自由文本低置信度归一为 UNKNOWN，连续两次 UNKNOWN 或操作失败由状态机升级人工。
- 流程管理页修复节点更新和模拟调用，并提供范围字段与完整节点类型。

## 新增迁移

- `V17__align_troubleshoot_schema_and_backfill_snapshots.sql`
- `V18__conversation_flow_steps.sql`
- `V19__flow_match_scope_and_priority.sql`
- `V20__controlled_flow_reply.sql`
- `V21__troubleshoot_flow_definitions.sql`

## 已验证

- 后端：`mvnw.cmd test`，26 个测试成功；Docker 未启动，Flyway PostgreSQL Testcontainers 测试跳过。
- 前端：`npm test`（1 项）、`npm run lint`、`npm run build` 均退出成功。
- `git diff --check` 无空白错误，仅报告工作区 CRLF 转换提示。

## 仍需在后续验收确认

- 使用真实 PostgreSQL 执行所有迁移并覆盖发布、续答、版本切换、人工升级的端到端路径。
- 将至少 3 至 5 条真实低风险流程及其成功、失败、未知、拒绝、安全停止分支纳入种子数据和端到端回归。
