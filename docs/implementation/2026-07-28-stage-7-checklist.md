# 阶段 7 实施清单

- [x] 前端、后端生产 Dockerfile 与 Compose 服务。
- [x] 前端反向代理 `/api` 与 `/public` 至业务后端。
- [x] Compose 注入依赖服务地址和必需启动密钥。
- [x] 保持现有 PostgreSQL、MinIO、Qdrant、OCR、Embedding 健康检查与备份脚本。
- [x] 后端单元测试和前端生产构建通过。

验证边界：当前 Windows 环境未安装 Docker，未能运行 `docker compose config` 或真实容器/浏览器端到端验收；上线前必须在 Docker 主机完成该项验收。
