# 阶段 6：人工交接闭环

## 已实现

- `HandoffPackageBuilder` 生成服务端不可变 schema v2 快照，不接受浏览器 summary 作为最终事实。
- 快照包含会话/产品上下文、消息和错误码、内部附件引用与 OCR 分析、answer trace/证据引用、流程会话与执行步骤、转人工原因、联系方式授权、渠道和 requestId。
- 快照不包含附件正文；创建和日志均不输出联系方式或客户消息全文。
- 内部队列支持租户隔离的列表、领取、关闭和内部备注；备注不会覆盖客户提交内容。
- Chatwoot 未作为强依赖；当前内部队列是主处理通道。

## 新增迁移

- `V22__handoff_notes.sql`

## 已验证

- 后端 `mvnw.cmd -B test`：29 个测试成功；Docker 未启动，PostgreSQL Testcontainers 测试跳过。
- 前端 `npm run lint`、`npm run build`：退出成功。
- `git diff --check`：无空白错误，仅有 CRLF 提示。

## 后续验收

- 在 PostgreSQL 环境验证迁移和跨租户备注访问拒绝。
- 以真实附件、OCR、流程会话和 answer trace 验证快照字段完整性。
- 如首家客户部署 Chatwoot，增加可选 adapter、签名 webhook 与重试演练，且保持内部队列可独立运行。
# 阶段 6 补充：内部队列状态与 SLA

- V25 增加 `priority`、`sla_due_at` 与租户/状态/SLA 索引。
- `HandoffRequest` 支持 `NEW`、`ASSIGNED`、`IN_PROGRESS`、`WAITING_USER`、`WAITING_PARTS`、`RESOLVED`、`CLOSED`、`FAILED_DELIVERY` 的受控迁移。
- 管理端可领取、推进状态、更新优先级/SLA、关闭和记录备注；交接包保持不可变。
- `HandoffRequestTest` 覆盖合法生命周期和非法状态跳跃。
- `HumanSupportAdapter` 与 `InternalQueueAdapter` 已接入创建工单链路；可选通道失败不会回滚内部工单，`HandoffDeliveryServiceTest` 覆盖该降级。

Chatwoot API/Webhook 是可选外部适配器，当前仍未配置或联调，不能当作本项已验收。
