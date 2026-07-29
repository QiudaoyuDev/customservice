# 阶段 2 实施清单

- [x] V11/V12 新增知识适用范围、索引生命周期、切块 FTS/元数据、OCR 审计、源文件 checksum 与可恢复处理任务字段；未改写历史迁移。
- [x] 上传同时校验扩展名、MIME、magic bytes、PDF 页数、DOCX 解压大小和图片像素；同租户相同源 checksum 默认拒绝并可显式创建重复修订。
- [x] 既有源文件可在文档锁内原子递增 revision number，并创建新的解析任务和适用范围记录。
- [x] PDF 提取保留页码，DOCX 提取标题和表格边界；切块持久化标题路径、页码、checksum、token 数和 JSON 元数据。
- [x] OCR 记录原始文本、归一文本及适配器提供的置信度、语言、页码。
- [x] ProcessingJob 使用 `FOR UPDATE SKIP LOCKED`、租约、心跳、退避重试和最大尝试次数；外部解析、Embedding、Qdrant 调用不包在领取事务中。
- [x] 发布先进入 `INDEXING`，索引及 Qdrant smoke query 成功才转换为 `PUBLISHED + READY`；最终失败保持不可检索。
- [x] RetrievalService 以 PostgreSQL FTS + Qdrant + RRF + 本地 rerank 检索；会话与管理检索页均通过相同服务端范围门禁，并在候选操作说明相冲突时拒绝确定回答。
- [x] 文档预览显示索引状态、标题路径和页码；检索页显示统一混合检索结果。

## 本地验证

- 后端：`JAVA_HOME='C:\Program Files\Java\latest\jdk-21'; .\mvnw.cmd test`：19 项通过，1 项 PostgreSQL Testcontainers 测试因本机无 Docker 跳过。
- 前端：`npm run lint`、`npm test`、`npm run build` 通过。
- `git diff --check` 通过；仅有仓库现有 CRLF 自动转换提示。

## 运行验证边界

Docker 依赖环境按当前任务要求暂不处理。因此 Flyway 在真实 PostgreSQL、MinIO 上传、OCR/Embedding/Rerank/Qdrant 调用和浏览器端到端流程尚未实际运行验证；阶段 2 保持进行中，不能据此开始阶段 3。
