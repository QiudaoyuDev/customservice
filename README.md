# 出海硬件厂商多语言 AI 售后诊断与客服平台

面向出海硬件厂商的设备售后诊断与知识运营产品方案。系统通过二维码识别设备型号、地区与版本，在受产品上下文约束的知识范围内提供多语言售后支持；当证据不足、存在安全风险或需要处理保修与退款时，将完整上下文交接给人工客服。

> 普通知识库只能回答"遥控器怎么配对"；本平台进一步解决"当前用户用的是哪个型号/硬件版本/销售区域/固件版本""故障现象是什么""应该依次检查什么""每一步进入哪个排障分支""哪些操作有断电/拆机/触电/数据丢失风险""何时停止 AI 排障并转人工/换货/返修""如何把 AI 已收集的信息完整交给人工"。

真正的产品壁垒不是模型或聊天页面，而是三部分：**产品级知识体系、结构化故障诊断流程、真实客服数据形成的持续优化闭环**。

---

## 目录

- [一、项目简介与核心功能](#一项目简介与核心功能)
- [二、技术架构与模块划分](#二技术架构与模块划分)
- [三、环境依赖与快速启动配置](#三环境依赖与快速启动配置)
- [四、目录结构概览](#四目录结构概览)
- [五、核心业务流程说明](#五核心业务流程说明)
- [六、相关文档与导航](#六相关文档与导航)

---

## 一、项目简介与核心功能

### 产品目标

- 为硬件厂商提供 7×24 小时、与具体型号和版本匹配的售后支持。
- 用可执行的故障诊断流程替代泛化的 FAQ 问答。
- 将说明书、错误码、维修记录与专家经验沉淀为可审核、可追溯的知识资产。
- 通过人工接管与安全规则，避免错误指引与高风险自助操作。

### 核心功能

| 能力 | 说明 |
| --- | --- |
| 设备识别 | 二维码绑定租户、型号、地区及可选批次或序列号；二维码令牌经 SHA-256 哈希存储，仅哈希落库。 |
| 受限检索 | 按型号、硬件版本、固件版本、地区、语言与有效期过滤知识（适用性过滤）。 |
| 分步诊断 | 针对高频故障提供带分支、预期结果和停止条件（安全停止）的排查流程，由确定性状态机驱动。 |
| 人工协同 | 自动附带对话、已尝试步骤、错误码、图片与引用知识进行转接，避免用户重复描述。 |
| 知识运营 | 文档解析、审核发布、回滚、过期提醒与质量指标分析，知识修订具备完整生命周期。 |
| 多语言 | 后端消息（`messages_zh` / `messages_en`）+ 前端 `i18next`（zh / en）双语支持。 |

### 当前实现范围（MVP）

以一家试点厂商、一个硬件品类、一个产品系列为起点：支持二维码绑定型号、文字/图片对话、文档知识库、高频故障流程与人工转接渠道。**首期不包含**自动退款换货、自动远程控制与未经审核的知识自动发布。

> 大模型调用由 `LLM_ENABLED` 开关控制，**默认关闭**。关闭时仍可运行意图分类、证据检索、诊断状态机与人工转接；开启后通过 OpenAI 兼容 Provider 提供生成式回答。

---

## 二、技术架构与模块划分

### 整体架构

```
终端用户（扫码）
   │  /public/*（无需登录，凭 QR 令牌 + 会话令牌）
   ▼
┌─────────────────────────── 后端 support-api (Spring Boot 3.3.2 / Java 21) ───────────────────────────┐
│  identity(认证/JWT) │ product(产品) │ qr(二维码绑定) │ knowledge(知识) │ conversation(会话/诊断)       │
│  troubleshoot(诊断流程) │ handoff(人工转接) │ llm(意图/Provider) │ analytics │ common/config/i18n       │
└───┬──────────┬───────────────┬───────────────┬───────────────┬────────────────┬──────────────────────┘
    │          │               │               │               │                │
 PostgreSQL   MinIO         Qdrant          OCR 服务         Embedding/       大模型 Provider
 (租户/用户/  (原始 PDF/     (知识块向量      (PaddleOCR        Rerank 服务      (OpenAI 兼容，
  产品/会话/   图片/附件)     与元数据)         适配器，本地)      Sentence-       外部唯一出口，
  流程/工单/                  │               │               Transformers    脱敏后调用)
  审计)                       │               │               BAAI/bge-m3)
    │          │               │               │               │
   Flyway    对象存储       向量检索        本地 OCR        本地推理(CPU/GPU)
   V1–V9      (私有 bucket)
```

### 后端模块（`repos/backend`，包名 `com.hardwareai.support`）

| 模块 | 职责 |
| --- | --- |
| `common` | 统一异常处理（`ApiExceptionHandler`）、请求上下文与访问日志（`RequestContextFilter` / `AccessLogFilter`）、当前用户（`CurrentUser`）。 |
| `config` | 外部化配置（`AppProperties`）、安全配置（`SecurityConfig`）、启动安全校验（`StartupSecurityValidator`，强制密钥）、存储配置、依赖健康探针（`ExternalDependencyHealthIndicator`）、国际化配置。 |
| `identity` | 认证与授权：`AuthController`、`JwtService`、`JwtAuthenticationFilter`、`UserAccount`（ADMIN / KNOWLEDGE_REVIEWER 角色）、`BootstrapDataInitializer`（库空时创建初始管理员）。 |
| `product` | 产品型号与别名管理：`ProductController`、`ProductModel`、`ProductModelAlias`。 |
| `qr` | 二维码绑定与解析：`QrController`、`QrBinding`（令牌哈希 + 有效期 + 吊销）。 |
| `knowledge` | 知识运营：`KnowledgeController`（上传/审核/发布）、`KnowledgeSearchController`（受限检索探针）、`KnowledgeDocument` / `KnowledgeRevision`（修订生命周期）、`KnowledgeChunk` / `KnowledgeChunker`、`DocumentTextExtractor`（PDFBox + POI 解析）、`OcrClient`、`ObjectStorage`（MinIO）、`VectorIndex`（Qdrant）、`LocalReranker`、`KnowledgeProcessingWorker`（异步处理）、`EvidenceService`、`ProcessingJob`。 |
| `conversation` | 终端用户会话与诊断编排：`ConversationController`（公开 API，消费已发布诊断流程）、`Conversation`、`ConversationMessage`、`MessageAttachment`、`ConversationProductContext`、`ConversationFeedback`、`ConversationAccessService`。 |
| `troubleshoot` | 诊断流程内容管理：`TroubleshootController`、`TroubleshootFlow`、`TroubleshootNode`、`TroubleshootStateMachine`（确定性分支与安全停止）、`TroubleshootTypes`。 |
| `handoff` | 人工转接工单：`HandoffController`、`HandoffRequest`（幂等键 + 上下文快照 + 处置结论）。 |
| `llm` | 意图分类（`IntentClassifier`）与生成式回答（`OpenAiCompatibleProvider`）。 |
| `analytics` | 运营事件与评测模型（`OperationalEvent`、`EvaluationModels`）。 |
| `i18n` | 后端本地化消息（`messages_zh.properties` / `messages_en.properties`）。 |

> 后端为**单实例、模块化**服务（非微服务拆分）；数据库 Schema 由 Flyway 迁移脚本（`src/main/resources/db/migration/V1__…V9__…`）管理，覆盖：领域模型、知识处理任务、知识运营范围、会话、LLM 审计、诊断流程、人工转接、评测、公开会话与转接包。

### 前端模块（`repos/frontend`，`hardware-ai-support-console`）

技术栈：React + TypeScript + Vite + Tailwind CSS + `react-router-dom` + `i18next`（中/英）。提供两个用户面：

- **终端用户面**：`/support/:qrToken`（`pages/SupportPage.tsx`）——扫码进入的多语言诊断对话。
- **运营管理后台**：`/console/*`（`console/ConsoleLayout.tsx`）——产品、二维码、文档、检索、诊断流程、人工转接六个管理页。

公共层：`lib/api.ts`（统一 API 访问，管理后台走 `/api` 带 Token，终端用户走 `/public`；含 SSE 流式回答解析）、`lib/auth.tsx`（JWT 登录态）、`lib/types.ts`（与后端契约对应的类型）。国际化：`i18n/`（zh / en）。

### 基础设施（`infra`，Docker Compose 基线 `hardware-ai-support`）

| 组件 | 镜像/实现 | 本地数据 |
| --- | --- | --- |
| 关系型数据库 | PostgreSQL 18 | 租户、用户、产品、知识版本、会话、流程、工单、审计。 |
| 对象存储 | MinIO | 原始 PDF、图片、用户附件、导出物与备份（默认私有 bucket）。 |
| 向量检索 | Qdrant | 已发布知识块及其产品/版本/地区等元数据。 |
| OCR | PaddleOCR Adapter（`infra/ocr`，本地构建） | 错误码、标签、屏幕文字、说明书扫描件识别。 |
| Embedding / Rerank | Sentence Transformers Adapter（`infra/embedding`，本地构建，`BAAI/bge-m3` + `bge-reranker-v2-m3`） | 本地生成向量并重排序。 |
| 大模型 | 统一 Provider Adapter（外部，唯一出口） | 仅发送脱敏后的用户文本与筛选后的证据。 |
| 业务应用 | `api`（构建 `repos/backend`）、`web`（构建 `repos/frontend`，Nginx） | — |

> 所有端口默认仅绑定 `127.0.0.1`；对外访问应由应用网关或反向代理统一提供 TLS、域名、访问控制与审计。详见 `infra/DEPLOYMENT.md`。

---

## 三、环境依赖与快速启动配置

### 环境依赖

| 类别 | 要求 |
| --- | --- |
| JDK | **21**（Maven enforcer 强制 `[21,22)`） |
| 构建工具 | 后端使用仓库内固定 Maven 3.9.11 Wrapper；前端使用 Node.js 20+ 与锁定依赖。 |
| 数据库 | PostgreSQL 16/18（生产由 `infra` 提供 18.4） |
| 本地 AI 依赖 | OCR、Embedding/Rerank、Qdrant、MinIO（由 `infra/compose.yaml` 提供；本机未启动时相关功能不可用语 `/actuator/health` 返回 503） |
| 容器（可选） | Docker Desktop + Docker Compose（用于启动完整基础设施或一键部署） |

### 方式一：本机前后端直启（开发，Windows）

适用于仅跑前后端、依赖外部/占位服务。本机一键脚本位于 `scripts/`：

```powershell
# 重启前后端（先停 8080 / 5173，再依次启动；可加 -Build / -Install）
.\scripts\start-all.ps1
.\scripts\start-all.ps1 -Build -Install

# 或分别启动
.\scripts\start-backend.ps1
.\scripts\start-frontend.ps1 -Install
```

手动启动步骤：

1. **前端**（Vite，端口 5173）
   ```powershell
   cd repos/frontend
   npm ci
   npm run dev                                            # → http://localhost:5173
   ```
   `vite.config.ts` 已将 `/api` 与 `/public` 代理到后端 `http://localhost:8080`。

2. **后端**（Spring Boot，端口 8080）——需先确保 PostgreSQL 可用
   ```powershell
   cd repos/backend
   $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'   # 必须为 JDK 21
   .\mvnw.cmd clean test package

   # 关键环境变量（应用实际读取的变量名见下）
   $env:SPRING_PROFILES_ACTIVE     = 'dev'
   $env:DATABASE_URL               = 'jdbc:postgresql://localhost:5432/hardware_ai_support'
   $env:DATABASE_USERNAME          = '<local database user>'
   $env:DATABASE_PASSWORD          = '<local database password>'
   $env:JWT_SECRET                 = '<至少 32 字节的密钥>'
   $env:QR_TOKEN_SECRET            = '<至少 32 字节的密钥>'
   $env:BOOTSTRAP_ADMIN_EMAIL      = '<administrator email>'
   $env:BOOTSTRAP_ADMIN_PASSWORD   = '<administrator password>'
   $env:MINIO_ENDPOINT             = 'http://localhost:9000'
   $env:MINIO_ACCESS_KEY           = 'minioadmin'
   $env:MINIO_SECRET_KEY           = 'minioadmin'
   $env:MINIO_BUCKET               = 'support-assets'
   $env:OCR_URL                    = 'http://localhost:18081'
   $env:EMBEDDING_URL              = 'http://localhost:18082'
   $env:RERANK_URL                 = 'http://localhost:18083'
   $env:QDRANT_URL                 = 'http://localhost:6333'
   $env:QDRANT_API_KEY             = '<若启用 Qdrant 鉴权>'
   $env:LLM_ENABLED                = 'false'     # 默认关闭；开启需配置 LLM_BASE_URL / LLM_API_KEY / LLM_MODEL

   java -jar target/support-api-0.1.0-SNAPSHOT.jar        # → http://localhost:8080
   ```

> 变量说明：后端通过 `application.yml` 的 `${VAR:默认值}` 读取上述变量；`infra/compose.yaml` 中的 `api` 服务也使用同一组变量名（如 `DATABASE_URL`、`MINIO_ENDPOINT`）。管理后台初始凭据由 `BOOTSTRAP_ADMIN_*` 在库空时创建一次。

### 方式二：Docker Compose 一键基础设施（推荐演示/试点）

```bash
cd infra
cp .env.example .env          # 必须替换为真实密钥；.env 永不提交 Git
sudo bash scripts/install-docker-centos.sh   # 仅首次安装 Docker（CentOS/RHEL 系）
bash scripts/bootstrap.sh     # 生成随机密钥
bash scripts/up.sh            # 启动全部服务
bash scripts/health.sh        # 健康检查
```

- API：`http://localhost:8080`（容器内 `api`）
- 前端（Nginx）：`http://localhost:8088`
- 管理后台入口：`http://localhost:8088/console`（或 `http://localhost:5173/console` 走本地前端）

数据库迁移由 Flyway 在应用启动时自动执行；Compose 中所有第三方镜像均固定为已验证版本，生产部署还应记录并校验镜像摘要。

---

## 四、目录结构概览

```
customservice/
├── README.md                     # 本文件
├── LOCAL_RUN.md                  # 本机（Windows）前后端启动与凭据说明
├── docs/                         # 产品与实施方案文档
│   ├── 海外硬件AI智能客服产品方案.md
│   ├── 海外硬件AI智能客服MVP功能拆解与实施方案.md
│   ├── UI方案.md
│   ├── 2026-07-28-initial-development-plan.md
│   ├── 2026-07-28-mvp-global-development-plan.md
│   ├── implementation/           # 实施细节文档（9 篇）
│   └── output/  tmp/             # 产出物与临时素材
│
├── infra/                        # 本地基础设施部署基线（Docker Compose）
│   ├── compose.yaml              # 服务编排：postgres/minio/qdrant/ocr/embedding/api/web
│   ├── .env.example              # 环境变量模板（务必替换）
│   ├── DEPLOYMENT.md             # 部署、安全、备份与限制说明
│   ├── ocr/                      # PaddleOCR 适配器（Dockerfile + requirements + app）
│   ├── embedding/                # Sentence Transformers 适配器（Dockerfile + requirements + app）
│   ├── postgres/                 # 初始化脚本（init/）
│   ├── scripts/                  # install-docker-centos / bootstrap / up / health / backup-postgres
│   ├── data/  backups/           # 持久化与备份挂载
│
├── scripts/                      # 本机一键启动（PowerShell）
│   ├── start-all.ps1             # 先停后启前后端
│   ├── start-backend.ps1
│   ├── start-frontend.ps1
│   └── _common.ps1               # 共用函数（按端口停进程等）
│
└── repos/
    ├── backend/                  # Spring Boot 后端（com.hardwareai.support）
    │   ├── src/main/java/com/hardwareai/support/{common,config,identity,product,qr,knowledge,conversation,troubleshoot,handoff,llm,analytics,i18n}
    │   ├── src/main/resources/
    │   │   ├── db/migration/      # Flyway V1__…V9__…（9 个迁移脚本）
    │   │   ├── i18n/              # messages_zh / messages_en
    │   │   ├── application.yml  application-dev.yml  application-prod.yml  application-test.yml
    │   │   └── logback-spring.xml
    │   ├── pom.xml                # 依赖与构建（Spring Boot 3.3.2 / Java 21）
    │   ├── Dockerfile  mvnw  mvnw.cmd
    │
    └── frontend/                 # React + Vite 前端（hardware-ai-support-console）
        ├── src/
        │   ├── app/Root.tsx       # 路由根
        │   ├── pages/             # Login / SupportPage（终端用户）
        │   ├── console/           # ConsoleLayout + Products/Qrs/Documents/Search/Flows/Handoffs 六页
        │   ├── components/ui.tsx  # 通用 UI 组件
        │   ├── lib/               # api.ts / auth.tsx / types.ts
        │   ├── i18n/              # index.tsx + locales/{zh,en}.ts
        │   ├── main.tsx  styles.css  index.css  vite-env.d.ts
        ├── package.json  vite.config.ts  tsconfig.json
        ├── tailwind.config.js  postcss.config.js
        ├── Dockerfile  nginx.conf
        └── dist/                  # 生产构建产物
```

---

## 五、核心业务流程说明

### 1. 二维码绑定与产品上下文识别

- **管理侧**：管理员（`ADMIN`）调用 `POST /api/qr-bindings` 为某产品型号创建绑定，系统生成随机令牌，仅将 **SHA-256 哈希** 存入 `qr_bindings`（可带批次/序列号/有效期），并返回明文令牌。可吊销（`POST /api/qr-bindings/{id}/revoke`）。
- **用户侧**：终端用户扫码后，前端用令牌调 `POST /public/qr/resolve` 解析出产品型号、名称、地区、批次等上下文（校验哈希、有效期与吊销状态）。
- **会话创建**：`POST /public/conversations` 以 QR 令牌作为**唯一权威**确定产品范围，生成会话并签发 `X-Conversation-Token`（会话访问令牌）；同时写入 `ConversationProductContext`（型号/硬件版本/固件版本/来源=QR）。

### 2. 知识运营（上传 → 解析 → 索引 → 发布）

1. 管理员上传文档（PDF/Office，`POST /api/documents`，`ADMIN`），文件存入 MinIO，创建 `KnowledgeDocument` 与初始 `KnowledgeRevision`（状态 `UPLOADED`）。
2. `KnowledgeProcessingWorker` 异步执行：`DocumentTextExtractor`（PDFBox/POI 提取文本）→ 可选 `OcrClient`（图片/扫描件）→ `KnowledgeChunker` 切片 → 调 Embedding 服务生成向量。
3. 向量与元数据写入 Qdrant（`VectorIndex`）。
4. 修订进入生命周期：`UPLOADED → DRAFT → REVIEW → APPROVED → PUBLISHED`，支持回滚与弃用（`DEPRECATED`），生命周期拒绝绕过（与诊断流程一致）。
5. 管理员可用 `POST /api/search` 在发布前验证**适用性过滤**与检索效果（`KnowledgeSearchController` + `LocalReranker`）。

### 3. 会话与诊断编排（终端用户）

- 用户发送消息（`POST /public/conversations/{id}/messages`，带 `X-Conversation-Token`，可附错误码与图片附件），附件经 ObjectStorage 落 MinIO。
- 系统执行：`IntentClassifier` 意图分类 → `EvidenceService` 在已发布知识中按**型号/硬件版本/固件版本/地区/语言/有效期**做受限检索 → 命中已发布诊断流程时由 `ConversationController` 消费。
- 回答通过 SSE 流式返回（`GET /public/conversations/{id}/answers/stream`；失败回退非流式）。`LLM_ENABLED=true` 时由 `OpenAiCompatibleProvider` 生成回答（仅发送脱敏文本与筛选证据）。

### 4. 结构化故障诊断流程

- **内容管理**（`TroubleshootController`，`/api/flows`）：管理员创建流程（关联触发意图、产品、地区、语言、固件区间），添加节点（`TroubleshootNode`，类型/提示/风险/预期输入/分支跳转/安全停止/引用来源），流程经 `DRAFT → REVIEW → APPROVED → PUBLISHED` 发布。
- **运行时编排**：已发布流程在 `ConversationController` 中由 `TroubleshootStateMachine` **确定性**驱动分支：
  - 分支由用户回复 `YES / NO / UNKNOWN` 选择 `branchYes / branchNo / branchUnknown`；
  - **安全停止**：当节点风险为 `HIGH`、用户选择 `REFUSE`、或连续失败 `≥ 2` 次时，直接进入 `HUMAN_ESCALATION`；
  - 缺少目标分支时同样升级人工。
- 提供 `POST /api/flows/{id}/simulate` 进行流程模拟与覆盖率校验（可达节点/不可达节点）。

### 5. 人工协同转接（Handoff）

- 触发升级时，`ConversationController` 创建 `HandoffRequest`（幂等键保证通知重试不重复建单），并将**完整上下文快照**（`packageSnapshot`：对话、已尝试步骤、错误码、图片、引用知识）一并写入。
- 管理员在 `/console/handoffs` 查看并 `claim`（置 `IN_PROGRESS`、分配处理人），处理后 `close` 并选择处置结论：`RESOLVED / WAITING_PARTS / WARRANTY / ABANDONED / DUPLICATE / PRODUCT_DEFECT`。

### 6. 安全与可观测性基线

- 启动时 `StartupSecurityValidator` 校验 `JWT_SECRET`、`QR_TOKEN_SECRET` 等密钥强度，不足则拒绝启动。
- 所有外部依赖（DB/MinIO/Qdrant/OCR/Embedding）通过 `ExternalDependencyHealthIndicator` 暴露至 `/actuator/health`；任一不可用返回 503。
- 请求链路带 `requestId`（`RequestContextFilter` / `AccessLogFilter`），前后端联调可通过响应头 `X-Request-Id` 串联。
- 大模型调用前执行 PII 脱敏、附件白名单与上下文最小化（唯一允许的外网出口）。

---

## 六、相关文档与导航

- [产品方案](./docs/海外硬件AI智能客服产品方案.md)
- [MVP 功能拆解与实施方案](./docs/海外硬件AI智能客服MVP功能拆解与实施方案.md)
- [UI 方案](./docs/UI方案.md)
- [初版开发记录](./docs/2026-07-28-initial-development-plan.md)
- [MVP 全球开发计划](./docs/2026-07-28-mvp-global-development-plan.md)
- [基础设施部署说明](./infra/DEPLOYMENT.md)
- [本机启动说明（Windows 开发机）](./LOCAL_RUN.md)

> 说明：本 README 依据当前仓库实际代码（后端 `repos/backend`、前端 `repos/frontend`、基础设施 `infra`）整理。环境与端口以 `application.yml`、`infra/.env.example`、`infra/compose.yaml` 为准；生产部署前请替换所有 `CHANGE_ME_*` 占位密钥。
