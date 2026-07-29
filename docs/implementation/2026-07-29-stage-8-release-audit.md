# 阶段 8：发布前审计

## 本地已完成验证

| 项目 | 命令 | 结果 |
| --- | --- | --- |
| 后端干净测试 | `repos/backend/mvnw.cmd -B clean test` | 33 成功，1 跳过 |
| 前端干净安装 | `repos/frontend/npm ci` | 成功 |
| 前端测试 | `npm test` | 2 成功 |
| 前端格式检查 | `npm run lint` | 退出成功 |
| 前端生产构建 | `npm run build` | 成功 |
| Diff 空白检查 | `git diff --check` | 无空白错误；仅 CRLF 提示 |
| 明文敏感信息扫描 | `rg` 对密码/API Key/secret | 仅环境变量、占位符、测试数据与表单字段 |

## 已交付范围

- 阶段 1：产品变体、固件、二维码与会话产品上下文。
- 阶段 2：知识生命周期、文档校验/OCR、混合检索和索引任务。
- 阶段 3：模型配置加密、受控回答、追溯和 SSE 基础链路。
- 阶段 4：图片附件分析、OCR 确认流程。
- 阶段 5：不可变流程快照、定义/版本、会话步骤与受控状态机。
- 阶段 6：不可变人工交接包、内部队列和备注。
- 阶段 7：隐私安全运营事件、看板和可执行检索评测。
- 阶段 8（非 Docker 范围）：会话/附件保留任务、V24 级联清理迁移、SSE 增量协议前后端测试、可执行前端格式检查与精确依赖锁定。

## 本轮明确排除或未验证项

1. **Docker/Compose 与真实外部依赖**：按本轮指令暂不处理。因此 PostgreSQL Testcontainers、Compose 健康检查、MinIO/Qdrant/OCR/Rerank/LLM 故障演练和浏览器 E2E 未完成，不能标记为生产环境验收通过。
2. **真实 PostgreSQL 迁移**：V14–V24 已编译进测试资源，但因 Docker 未启动，尚未由 PostgreSQL 实例实际执行。
3. **前端依赖风险**：`npm ci` 报 3 个漏洞（2 moderate、1 critical）。需要在单独依赖升级任务中定位经由依赖并评估升级；未执行 `npm audit fix --force`。
4. **可选集成**：Chatwoot 与 Langfuse 均保持非强依赖，尚未配置客户环境验证。

## 发布建议

在具备 Docker 和隔离测试凭据的环境，按 README/LOCAL_RUN 完成 Compose 启动、迁移、真实依赖健康检查、核心用户旅程与故障演练后，才可将 MVP 标记为试点发布就绪。
