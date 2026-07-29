# 阶段 7：运营分析与评测

## 已实现

- `operational_events` 只存储白名单属性；会话、消息、附件、检索、流程、交接和回答都写入隐私安全事件。
- `/api/analytics/overview` 提供租户级事件计数、回答数、人工交接、无证据、冲突和平均回答延迟。
- 管理端新增 Analytics 页面，直接调用真实 overview API。
- 评测用例新增产品/变体/硬件/固件范围；评测运行调用生产 `RetrievalService`，持久化 `evaluation_runs` 与 `evaluation_results`。
- 评分由期望 outcome 与最少引用数共同决定；结果保留实际引用数量与冲突状态。

## 新增迁移

- `V23__evaluation_scope.sql`

## 已验证

- 后端 `mvnw.cmd -B test`：30 个测试成功；Docker 未启动，PostgreSQL Testcontainers 测试跳过。
- 前端 `npm run lint`、`npm run build`：退出成功。
- `git diff --check`：无空白错误，仅有 CRLF 提示。

## 后续验收

- 在真实 PostgreSQL/Qdrant 数据集上运行评测，建立上线前的最低分数阈值。
- 验证跨租户看板、评测用例和运行详情不可读取。
- Langfuse 仍为可选可观测性集成，不得阻断回答或评测。
