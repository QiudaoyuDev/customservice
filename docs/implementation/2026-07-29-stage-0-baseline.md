# 阶段 0：工程基线、安全与可复现构建

## 已完成

- 删除已跟踪的运行日志、IDE 工程文件、模块文件及前端锁文件备份；`.gitignore` 覆盖这些本地运行产物。
- 新增 `.editorconfig`，后端 Maven Wrapper 固定下载 Maven 3.9.11；前端核心依赖从 lockfile 固定到精确版本。
- 明确 `dev`、`test`、`prod` profile：生产和测试拒绝默认或弱密钥；开发环境的连接和初始管理员凭据仍必须由本机环境提供。
- PostgreSQL 连接池、MinIO、Qdrant、OCR、Embedding、Rerank 和 OpenAI-compatible LLM 客户端均使用连接、读取和总请求超时配置。
- 统一错误响应为 `timestamp`、`code`、`message`、`requestId`、`details`；日志不再记录登录邮箱、外部响应异常详情、API key、令牌、联系方式或附件正文。
- 前端移除预填账号密码和本地伪登录，改为调用真实认证接口。
- Compose 固定第三方镜像版本，增加服务健康检查与 API 的就绪依赖；新增 GitHub Actions、前端 API 客户端测试和可选 Testcontainers PostgreSQL/Flyway 迁移测试。

## 验证边界

- Testcontainers 测试仅在 Docker 可用时运行；无 Docker 的环境会跳过该集成测试。
- Compose 配置与完整容器健康检查需要 Docker Desktop 或兼容 Docker 环境。阶段 0 的本机验证应记录实际 Docker 可用性，不能以静态审查替代。
- 镜像固定为版本标签；生产发布仍需要在部署清单中锁定并复核 image digest。
