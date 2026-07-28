# 本地基础设施部署基线

这套部署基线用于开发、演示和单机试点。除配置的大模型 API 外，业务数据、文档/图片、向量检索、OCR、Embedding 和 Rerank 都运行在客户本地或客户私有云中。

## 组件与职责

| 组件 | 服务 | 本地数据 |
| --- | --- | --- |
| 关系型数据库 | PostgreSQL 18 | 租户、用户、产品、知识版本、会话、流程、工单、审计。 |
| 对象存储 | MinIO | 原始 PDF、图片、用户附件、导出物和备份。 |
| 向量检索 | Qdrant | 已发布知识块及其产品/版本/地区等元数据。 |
| OCR | PaddleOCR Adapter | 本地识别错误码、标签、屏幕文字、说明书扫描件。 |
| Embedding / Rerank | Sentence Transformers Adapter | 本地生成向量与对候选知识重排序。 |
| 大模型 | 后续应用统一 Provider Adapter | 唯一允许的外部出口；只发送脱敏后的用户文本和筛选后的证据。 |

> 注意：开源不等于没有许可证义务。MinIO 的许可证与未来是否将其作为闭源产品的一部分分发有关；在确定商业交付模式前，应由法务/客户采购确认每个组件与模型的许可证。

## 部署方式

### CentOS/RHEL 系服务器：Docker Compose

适用场景：开发环境、售前演示、首家试点客户、低到中等并发。

所有端口默认只绑定 `127.0.0.1`，不直接暴露到公网。对外访问应由后续应用网关或客户已有反向代理统一提供 TLS、域名、访问控制和审计。

```bash
cd infra
sudo bash scripts/install-docker-centos.sh  # 仅首次安装 Docker 时执行
bash scripts/bootstrap.sh
bash scripts/up.sh
bash scripts/health.sh
```

支持的首选系统：CentOS Stream 9、Rocky Linux 9、AlmaLinux 9、RHEL 9 或其他兼容 `dnf/yum` 的发行版。生产服务器建议使用 Rocky Linux / AlmaLinux / RHEL 等有长期维护策略的发行版；CentOS Linux 传统版本已停止维护，不建议作为新的生产基线。

Compose 挂载已增加 `:Z` SELinux 标签。若客户安全规范禁止容器修改挂载标签，应由系统管理员预先为 `infra/postgres/init` 和 `infra/backups` 配置正确的 SELinux context，而不是关闭 SELinux。

首次启动 OCR 与 Embedding 服务会下载模型，速度取决于服务器外网和模型源。完成第一次下载后，模型存放在 Docker volume 中。正式离线交付时，应先在受控环境下载并校验模型，再将模型卷或模型目录带入目标网络。

### 生产与多客户：Kubernetes

当满足任一条件时，应从 Compose 迁移到 Kubernetes/客户现有容器平台：

- 需要高可用或跨节点存储。
- OCR/Embedding 需要独立 GPU 调度。
- 多个产品线/客户共享平台实例。
- 需要完善的监控、网络策略、备份、灰度升级与灾备。

迁移顺序：先拆 OCR/Embedding Worker，再拆 Qdrant 与对象存储；核心业务应用仍可以保持模块化 Java 服务。

## 服务器建议

### 1. 开发机

| 资源 | 建议 |
| --- | --- |
| CPU | 8 核 |
| 内存 | 16 GB，建议 32 GB |
| 磁盘 | 200 GB SSD |
| GPU | 不要求 |
| 系统 | Rocky Linux 9 / AlmaLinux 9 / RHEL 9；开发机可使用同类虚拟机 |

### 2. 首家客户单机试点

| 资源 | 建议 |
| --- | --- |
| CPU | 16 vCPU |
| 内存 | 64 GB |
| 磁盘 | 1 TB NVMe SSD，定期备份到独立介质/对象存储 |
| GPU | 不要求；OCR/Embedding 可使用 CPU。若图片和文档吞吐高，增加 1 块 24 GB 显存 NVIDIA GPU。 |
| 网络 | 仅应用网关可访问公网；数据库、Qdrant、MinIO 不暴露公网。 |

### 3. 初期生产建议

将数据和计算分开：

- 数据节点：8-16 vCPU、32-64 GB 内存、1-2 TB NVMe，运行 PostgreSQL、MinIO、Qdrant。
- 应用/AI 节点：8-16 vCPU、32 GB 内存，运行 Java 应用、OCR、Embedding；有性能需求时配 24 GB 显存 GPU。
- 备份节点/客户存储：独立于运行节点，保存 PostgreSQL dump、MinIO 镜像/复制、Qdrant snapshot。

实际规格必须根据文档数量、图片大小、并发会话、目标语言和本地模型压测结果调整。首期不要为了“可能的高并发”过度采购硬件。

## 安全基线

1. 不将 PostgreSQL、Qdrant、MinIO 直接映射到公网。
2. 运行 `bootstrap.sh` 生成随机密钥；`.env` 永不提交 Git。
3. 生产环境固定容器镜像摘要或已验证版本，不使用 `latest`；本基线的 `latest` 仅为首次验证方便。
4. Qdrant 必须配置 API key；生产部署还应使用 TLS 和网络策略。Qdrant 默认无认证，不能仅依赖内网隔离。
5. 对象存储 bucket 默认私有，应用通过短期签名 URL 访问文件。
6. 大模型调用前执行 PII 脱敏、附件白名单和上下文最小化。

## 备份与恢复

```bash
cd infra
bash scripts/backup-postgres.sh
```

这会写入 `infra/backups/postgres/`。此外必须制定：

- MinIO bucket 的定期镜像/复制策略。
- Qdrant collection snapshot 和恢复演练策略。
- `.env` 密钥的受控备份与轮换策略。
- 至少一次从空环境恢复 PostgreSQL、对象存储和向量索引的演练。

## 当前限制

- Compose 文件是单节点基线，不是高可用生产方案。
- OCR Adapter 已限定为文字/标签提取，不输出硬件故障结论。
- Embedding 模型名称可在 `.env` 中替换；上线前必须用真实多语言工单评测其召回率和许可证。
- 大模型 Provider Adapter、Java 业务应用、知识索引任务和人工渠道集成将在下一阶段实现。
