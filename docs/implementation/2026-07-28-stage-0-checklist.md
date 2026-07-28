# 阶段 0 实施清单

- [x] 固定 JDK 21 与 Maven 3.9+ 构建入口。
- [x] 配置 dev、test、prod profile，并在非 dev 环境拒绝弱密钥和默认密码。
- [x] 加入 PostgreSQL、MinIO、OCR、Embedding、Qdrant 的可观测健康检查。
- [x] 使用 `FOR UPDATE SKIP LOCKED` 原子领取持久化任务；外部调用不再包在长事务中。
- [x] 增加请求 ID、统一错误码和避免敏感内容的结构化日志。
- [x] 增加配置安全回归测试。
- [x] 后端 `mvn test` 与前端生产构建通过。

未覆盖：真实 PostgreSQL 并发 Worker、MinIO/OCR/Embedding/Qdrant 故障与重试行为需在阶段 7 Compose 环境做集成验证。
