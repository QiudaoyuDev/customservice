# 阶段 3 实施清单

- [x] V13 建立模型配置加密元数据，API Key 使用 AES-256-GCM ciphertext、nonce 和 keyVersion；主密钥只来自环境配置。
- [x] 管理 API 与 React 控制台支持模型配置、租户默认模型和不泄密的连接测试。
- [x] `SupportOrchestratorService` 负责 evidence 门禁、模型调用、模板降级、助手消息、trace 与 citation 持久化。
- [x] 模型必须返回结构化 JSON；服务端只接受本次 evidence 的 `C1..Cn` 引用，非法结果自动降级。
- [x] 确定性安全规则覆盖起火、冒烟、烧焦、触电、进水、高温、鼓包、拆机、高压和电源损坏；未知意图不再默认咨询。
- [x] 历史接口携带 USER/ASSISTANT/SYSTEM sender；SSE 发送 `meta`、`delta`、`citations`、`done` 或 `error`，并限制同会话并发回答。

## 验证

- 后端 `mvnw.cmd test`：21 项通过，1 项 Testcontainers 因 Docker 暂不处理跳过。
- 前端 `npm test`、`npm run lint`、`npm run build` 通过。
- `git diff --check` 通过。
