# customservice：Codex 全量开发总指令

**项目仓库**：`QiudaoyuDev/customservice`  
**基准分支**：`master`

**目标**：将现有演示型骨架升级为可供首家海外硬件厂商试点的 AI 售后诊断与智能客服 MVP。

---

## 1. 你的角色

你是该项目的主程和架构实施者。请在现有代码上增量开发，不要另起一个平行项目，不要用新的脚手架覆盖现有实现，也不要把系统重构成微服务。

技术栈保持：

- 后端：Java 21、Spring Boot、Spring Security、Spring Data JPA、Flyway。
- 前端：React、TypeScript、Vite、Tailwind CSS。
- 数据库：PostgreSQL。
- 对象存储：MinIO。
- 向量数据库：Qdrant。
- 本地 AI 服务：OCR、Embedding、Rerank。
- 外部生成模型：OpenAI-compatible API。
- 部署：Docker Compose，后续预留 Kubernetes，但本轮不实施 Kubernetes。

本项目不是通用聊天机器人。产品定位是：

> 面向海外硬件厂商、受具体产品型号/硬件版本/固件版本/地区约束的 AI 售后诊断平台。

系统必须做到：有证据才回答；多步故障必须走受控状态机；无证据、知识冲突、高风险、连续失败或用户要求人工时必须停止自助并交接人工。

---

## 2. 开始开发前必须执行

先在仓库根目录执行并记录结果：

```powershell
$env:JAVA_HOME='D:\CRRC\jdk21'
$env:MAVEN_HOME='D:\dev-tools\apache-maven-3.9.16-bin\apache-maven-3.9.16'
$env:Path="$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

java -version
mvn -version
node -v
npm -v
git status --short
git log -5 --oneline
```

然后执行当前基线验证：

```powershell
cd repos/backend
mvn -B test

cd ../frontend
npm ci
npm run build


```

要求：

1. 不覆盖用户已有未提交修改。
2. 不执行 `git reset --hard`、`git clean -fd`、强制 checkout 或大范围格式化。
3. 先阅读现有 `README.md`、`docs/`、Flyway 迁移、前后端入口和当前测试。
4. 建立 `docs/implementation/2026-07-29-codex-execution-checklist.md`，逐项记录：现状、缺口、修改文件、验证结果、剩余风险。
5. 本轮不要自动 commit、push、创建 PR；除非用户后续明确要求。

---

## 3. 已确认的当前问题

开发前重新核验以下问题，确认后在 checklist 中标记：

### P0：真实链路断点

1. Qdrant 已有写入代码，但在线回答仍只使用 PostgreSQL 的整句 `LIKE` 查询；向量召回、混合召回和 Rerank 没有进入用户回答链路。
2. 用户消息落库，但 AI 回答没有保存为 `ASSISTANT` 消息；`answer_traces`、`answer_citations`、`model_configurations` 已建表但没有完整实体、Repository、Service 和运行时写入。
3. SSE 接口只是一次性返回完整 Answer，不是真正的逐块流式输出，也没有取消、超时和断线恢复语义。
4. 图片上传目前只保存附件，没有 OCR/视觉处理结果进入问题理解和回答上下文。
5. 证据回答在 LLM 关闭或失败时直接返回第一段知识块，缺少可读回答、证据阈值、冲突判断和引用校验。
6. 意图识别和诊断回复归一化主要依赖中英文 substring，容易误判，不能覆盖目标市场语言。
7. 故障流程存在发布状态，但没有真正的不可变流程版本；进行中的会话也没有固定 `flowVersionId`。
8. 流程匹配未使用会话中的硬件版本和固件版本，`firmwareMin/firmwareMax` 当前基本未参与匹配。
9. 人工交接的 `packageSnapshot` 当前基本等于用户填写的 summary，没有包含产品上下文、完整会话、附件、引用、诊断步骤和失败原因。
10. 前端用户页存在硬编码客服测试账号/密码入口，必须删除。

### P0：知识与产品适用范围不足

1. 产品模型仍然偏平面，缺少清晰的产品变体、硬件 revision、地区变体与固件版本语义。
2. `KnowledgeRevision` 只有单个 `productModelId + region`，缺少硬件版本、固件范围、有效期和多产品适用关系。
3. 知识修订号固定为 1，缺少“基于已有文档创建新修订”的正式入口。
4. 知识发布先改变数据库状态、再异步索引，可能出现“数据库已发布但向量索引尚未成功”的不一致窗口。
5. 知识块缺少稳定的页码、标题路径、表格/图片引用、token 数、checksum 和关键词检索向量。
6. 文档上传只检查浏览器 Content-Type，缺少文件签名、扩展名、压缩炸弹、恶意文件扫描接口和重复文件识别。

### P1：工程与交付问题

1. Compose 中 MinIO、Qdrant、MinIO Client 使用 `latest`，前端多项依赖使用 `latest`；私有化部署不可接受浮动版本。
2. 仓库中存在运行日志文件，`.gitignore` 未统一忽略 `*.log`、IDE 文件和本地生成物。
3. 当前仓库没有 GitHub Actions CI。
4. 后端 Controller 直接协调多个 Repository，业务编排集中在 Controller，应迁入 application service。
5. 前端 API 大量使用 `any`，没有 OpenAPI 生成类型，也没有单元测试或浏览器 E2E。
6. Analytics/Evaluation 数据结构已有部分骨架，但没有完整写入、查询、看板和回归执行链路。

---

## 4. 固定架构决策

### 4.1 保持模块化单体

后端继续使用一个 Spring Boot 应用，但按领域明确分层：

```text
com.hardwareai.support
├─ identity
├─ tenant
├─ product
├─ qr
├─ knowledge
│  ├─ application
│  ├─ domain
│  ├─ infrastructure
│  └─ web
├─ retrieval
├─ conversation
├─ orchestration
├─ troubleshoot
├─ handoff
├─ analytics
├─ evaluation
├─ integration
│  ├─ llm
│  ├─ chatwoot
│  ├─ langfuse
│  └─ malware
└─ common
```

不要为了目录形式一次性搬动所有文件。优先将新增逻辑放入正确分层，旧 Controller 在修改时逐步变薄。

### 4.2 开源项目使用策略

1. **RAGFlow**：首个 MVP 不整体嵌入 RAGFlow，不让它取代现有业务数据库、产品范围和知识审核模型。将其作为复杂文档解析/检索的参考或后续可插拔 `KnowledgeEngineAdapter`。当前自研链路先打通。
2. **Chatwoot**：首期保留内部人工队列；完成稳定交接包后，实现可选 `ChatwootHandoffAdapter`，通过 API Inbox、Conversation/Message API 和签名 Webhook 同步人工状态。不要把产品知识和诊断状态放入 Chatwoot 作为主数据。
3. **Langfuse**：作为可选 observability profile 接入，用于 LLM/retrieval/flow trace、Prompt 版本和评测，不得成为回答主链路的强依赖。Langfuse 不可用时用户回答必须正常降级。
4. **Spring AI**：现阶段 Spring Boot 为 3.3.2。不要直接升级或引入当前 Spring AI，先写 ADR 评估兼容性。MVP 可以保留 RestClient 适配器，但必须抽象 `EmbeddingProvider`、`VectorStoreAdapter`、`ChatModelProvider`、`RerankProvider`。确认兼容并完成回归后才允许迁移。

### 4.3 数据一致性原则

- 业务数据库是知识发布状态、产品适用范围和会话状态的唯一事实源。
- Qdrant 只保存可检索副本，不保存业务状态的唯一版本。
- 发布必须变为两阶段：`APPROVED -> INDEXING -> PUBLISHED`。
- 只有索引成功并通过最小检索验证后才进入 `PUBLISHED`。
- 下架先使数据库立即不可检索，再异步删除向量；即使向量删除失败，检索过滤也必须阻止返回。
- 任何外部调用不得长时间占用数据库事务。

---

## 5. 分阶段开发计划

每一阶段都必须完成：数据库迁移、后端、前端、测试、文档、真实验证。不得只提交实体或空页面。

## 阶段 0：工程基线、安全清理和可复现构建

### 任务

1. 固定依赖和容器版本：
   - 移除 Compose 中所有 `latest`。
   - 将生产镜像固定到已验证版本，最好同时记录 digest。
   - 将 `package.json` 中 `react`、`react-dom`、TypeScript、Vite 等 `latest` 改为 lockfile 中当前实际版本。
2. 增加 Maven Wrapper，统一 `./mvnw` / `mvnw.cmd`。
3. 增加 `.editorconfig`，补全 `.gitignore`：`*.log`、`.idea/`、`*.iml`、本地环境、构建产物。
4. 删除前端所有硬编码账号密码和演示登录捷径。
5. 增加 `springdoc-openapi`，输出 OpenAPI；前端通过 `openapi-typescript` 或同类工具生成 API 类型，逐步替换 `any`。
6. 增加后端格式/静态检查和前端 lint/test scripts。
7. 建立 GitHub Actions：
   - backend test；
   - frontend `npm ci && build && test`；
   - Docker Compose config 校验；
   - 禁止提交明文密钥和大型日志。
8. 为外部服务统一超时、连接池、重试和 circuit breaker；不能对 4xx 重试。
9. 统一错误响应：`code/message/requestId/details/timestamp`。
10. 为公共接口增加基础限流抽象；Compose 的 Nginx 增加上传、请求频率和安全 Header。

### 验收

- 全新环境可按照 README 一次构建成功。
- 不配置强密钥时 prod 启动失败。
- 仓库不再包含运行日志和测试密码。
- CI 对每次提交自动验证后端、前端和迁移。

---

## 阶段 1：领域服务与产品上下文重构

### 数据模型

使用新增 Flyway 迁移，禁止修改旧迁移：

1. 新增 `product_variants`：
   - `id, tenant_id, product_model_id, region, hardware_revision, sku, status, valid_from, valid_to`。
2. 新增 `firmware_versions`：
   - `id, product_variant_id, version, release_date, status, checksum, notes`。
3. `qr_bindings` 增加可选 `product_variant_id` 和 `initial_firmware_version`。
4. `conversation_product_contexts` 增加：
   - `product_variant_id`；
   - `hardware_revision`；
   - `firmware_version`；
   - `source`；
   - `confirmed_by_user`；
   - `created_at/closed_at`。

### 后端

1. 新增 `ProductApplicationService`、`QrApplicationService`、`ConversationContextService`。
2. Controller 不再直接组合多个 Repository。
3. 所有产品读取都必须经过 tenant scope。
4. 二维码继续使用高熵 opaque token + SHA-256 hash，不在二维码里暴露 tenant UUID、型号和内部 ID。
5. 二维码解析返回：品牌/型号/变体/地区/产品图片/可选硬件版本，不返回内部敏感字段。
6. 产品归档后禁止新建二维码和新会话，但历史会话仍可审计。
7. 用户切换产品时结束旧 context，并清除进行中的诊断状态。

### 前端

- 产品页支持型号、变体、硬件 revision、地区、固件范围。
- QR 页面不再要求手填 UUID，使用下拉和搜索。
- 用户扫码页展示可读产品信息；产品无法识别时提供受控手工选择。

### 验收

- 两个租户使用同名型号时完全隔离。
- 同一型号不同硬件 revision/地区建立不同 context。
- 无法通过前端修改 tenant 或绑定其他租户产品。

---

## 阶段 2：知识生命周期、文档处理和混合检索

这是当前最优先的业务阶段。

### 数据模型

1. 新增 `knowledge_revision_applicability`：
   - `revision_id, product_model_id, product_variant_id, region, hardware_revision, firmware_min, firmware_max, valid_from, valid_to`。
2. `knowledge_revisions` 增加：
   - `index_status`：`NOT_INDEXED/INDEXING/READY/FAILED/REMOVING`；
   - `content_checksum`；
   - `parser_version`；
   - `index_version`；
   - `failure_code/failure_detail`；
   - `published_at/deprecated_at` 完整索引。
3. `knowledge_chunks` 增加：
   - `title_path`；
   - `page_from/page_to`；
   - `content_checksum`；
   - `token_count`；
   - `metadata jsonb`；
   - PostgreSQL `tsvector` 或维护的 searchable text；
   - 唯一约束 `(revision_id, chunk_no)`。
4. 增加“从已有文档创建新修订”接口，revision number 必须数据库内原子递增。

### 文档处理

1. 上传校验：扩展名 + MIME + magic bytes；限制 PDF 页数、DOCX 解压大小和图片像素。
2. 对相同 tenant + checksum 去重提示，但允许显式创建新修订。
3. PDF/DOCX 切块按标题、段落、表格和页码，不用固定字符粗切。
4. OCR 保存：原始 OCR、归一文本、置信度、语言、页码。
5. `ProcessingJob` 增加：租约、heartbeat、nextRetryAt、errorCode、maxAttempts。
6. 领取任务继续使用 `FOR UPDATE SKIP LOCKED`，但外部 OCR/Embedding/Qdrant 调用必须在事务外。

### 检索服务

新增独立 `retrieval` 模块：

```java
public interface RetrievalService {
    RetrievalResult retrieve(RetrievalRequest request);
}
```

`RetrievalRequest` 必须由服务端构造，包含：

- tenantId；
- productModelId/productVariantId；
- region；
- hardwareRevision；
- firmwareVersion；
- locale；
- userQuestion；
- errorCode；
- topK/threshold。

实现链路：

1. 服务端适用范围硬过滤。
2. 精确召回：型号、错误码、按钮名、接口名、固件号。
3. PostgreSQL FTS/trigram 关键词召回。
4. Qdrant 向量召回，payload 必须包含 tenant/product/variant/region/locale/revision/indexVersion/status。
5. Reciprocal Rank Fusion 或明确的加权合并。
6. 调用本地 Rerank。
7. 去重、最低阈值、最大上下文长度。
8. 返回结构化 Evidence：chunkId、revisionId、documentTitle、page、titlePath、score、applicability、excerpt。
9. 检测同一问题的高分证据是否结论冲突；冲突时禁止生成确定答案。

删除当前 `LIKE '%整句问题%'` 作为唯一在线检索路径。可以保留它作为极低级降级，但不得作为默认。

### 发布一致性

1. publish 请求只将 revision 置为 `INDEXING` 并创建索引任务。
2. 索引任务成功后运行一条最小 smoke query，再原子更新为 `PUBLISHED + READY`。
3. 失败保持 `APPROVED/FAILED`，前台不可召回。
4. deprecated 后数据库过滤必须即时生效，向量删除异步重试。

### 前端

- 文档上传向导：文件、语言、适用型号/变体/地区/固件范围、责任人、复审日期。
- 文档预览显示页码、标题路径、块和解析警告。
- 检索验证页展示 lexical/vector/rerank 分数和过滤条件。
- 发布页明确显示 `INDEXING/READY/FAILED`。

### 验收

- 自然语言改写仍能命中正确知识。
- 相似型号、错误地区、错误硬件 revision、超出固件范围、未发布知识不能召回。
- 发布索引失败时不会出现“已发布但不可用”的假状态。
- 每个证据可回到原始文档页码。

---

## 阶段 3：模型配置、编排服务和真实可追溯回答

### 模型配置

1. 实现现有 `model_configurations` 表对应实体和管理 API。
2. API Key 使用 AES-256-GCM envelope encryption：
   - master key 只来自环境/KMS；
   - 数据库只保存 ciphertext、nonce、keyVersion；
   - API 永远只返回 `configured=true`，不返回明文。
3. 配置项：providerType、baseUrl、modelName、visionModel、timeout、temperature、maxTokens、enabled、isDefault。
4. 增加“测试连接”接口，但测试日志不得包含 Key 或完整请求正文。

### 编排服务

新增 `SupportOrchestratorService`，`ConversationController` 只做协议转换：

```text
加载会话并鉴权
→ 加载活动产品上下文
→ 保存用户消息
→ 安全规则
→ 意图分类
→ 诊断流程匹配
→ 或混合检索
→ 证据门禁
→ LLM grounded generation
→ 引用校验
→ 保存 assistant message/trace/citations
→ 流式返回
```

### 意图与安全

1. 确定性安全规则先执行，覆盖：起火、冒烟、烧焦气味、触电、进水、异常高温、鼓包、拆机、高压、电源损坏。
2. 其余意图可使用规则 + 模型分类，但模型输出必须 JSON Schema，并限定枚举。
3. 分类失败返回 `UNKNOWN`，不能默认进入普通咨询。
4. 保修、退款、换货只解释已发布政策，不能承诺资格。
5. Prompt injection 不能改变 tenant/product scope、系统安全规则、工具权限和引用要求。

### 回答生成

1. 输入只包含经过门禁的证据，不把整个文档或无关历史发给模型。
2. Prompt 要求：
   - 只基于 Evidence；
   - 不足则明确说不知道；
   - 不创造刷机、拆机或维修步骤；
   - 回答语言与用户一致；
   - 每个事实结论绑定 citation key。
3. 模型输出结构化 JSON：`answer, citations[], followUpQuestion, resolutionCandidate, handoffRecommended`。
4. 服务端验证 citation 是否来自本次 Evidence，非法引用直接降级。
5. 保存：
   - assistant message；
   - answer trace；
   - citations；
   - retrieval candidates/selected evidence；
   - prompt version/model config；
   - latency/token/finish reason/outcome。
6. 模型失败时生成基于模板的证据摘要或转人工，不直接无提示返回第一块原文。

### 流式输出

使用 Spring MVC `SseEmitter` 的异步 executor 或兼容方案真正发送：

- `meta`：answerId/traceId/intent；
- 多个 `delta`；
- `citations`；
- `done`；
- `error`。

用户断开后取消下游生成；同一会话同一时刻最多一个活动回答。

### 验收

- 历史接口同时返回 USER/ASSISTANT/SYSTEM 消息。
- 任意回答可以追溯到知识版本、模型、Prompt 和 retrieval trace。
- 无证据、冲突、模型超时、Rerank 失败都有明确降级。
- SSE 是多事件，而不是单次包装。

---

## 阶段 4：图片/OCR/视觉辅助链路

1. 上传附件后创建 `attachment_processing_jobs` 或统一任务类型。
2. 保存 `attachment_analysis`：OCR text、候选型号、序列号、错误码、指示灯描述、confidence、requiresConfirmation。
3. 首期只允许：
   - 标签 OCR；
   - 屏幕错误码；
   - 屏幕提示文本；
   - 指示灯颜色/闪烁描述辅助。
4. 识别结果必须让用户确认后才能改变产品上下文或进入错误码流程。
5. 图片不得自动决定拆机、刷机、换件或安全结论。
6. EXIF 清理、签名校验、像素限制、私有对象、短期签名 URL、保留期删除。

验收：模糊图片请求重拍；错误 OCR 不会自动进入错误流程；附件处理失败不丢失用户文字消息。

---

## 阶段 5：不可变故障流程版本与受控状态机

### 数据模型

将当前 flow 拆为：

- `troubleshoot_flow_definitions`：稳定业务标识、tenant、title。
- `troubleshoot_flow_versions`：version、status、适用范围、trigger、owner、publishedAt。
- `troubleshoot_nodes` 归属 flowVersion。
- `conversation_flow_sessions`：conversationId、flowVersionId、currentNodeKey、failureCount、status、startedAt、endedAt。
- `conversation_flow_steps`：nodeKey、normalizedReply、rawMessageId、result、createdAt。

已发布版本不可修改；修订必须 clone 成新 DRAFT 版本。进行中会话固定旧版本。

### 流程匹配

必须同时考虑：tenant、product/variant、region、locale、hardwareRevision、firmwareVersion、intent、trigger phrase/errorCode、优先级。

### 节点与验证

节点类型固定：`QUESTION, OBSERVE, OPERATION, VERIFY, DECISION, HUMAN_ESCALATION, END`。

发布前验证：

- 唯一 start node；
- 所有必需分支完整；
- 所有分支目标存在；
- 无不可控死循环；
- 所有节点可达或明确允许；
- 至少一个 END/HANDOFF；
- 高风险节点强制 stop；
- sourceRefs 指向已发布且适用的知识；
- 每个主要分支都有模拟测试。

### 回复归一化

1. 按钮输入直接使用受控 enum。
2. 自由文本使用多语言规则 + 可选模型 JSON 分类。
3. 低置信度返回 UNKNOWN，并再次询问，不得猜测 YES。
4. 用户拒绝、连续两次失败、UNKNOWN 超阈值、安全风险立即进入人工判断。

### 验收

至少实现 3–5 条真实低风险流程，并为每条流程测试成功、失败、未知、拒绝、安全停止、转人工、版本切换。

---

## 阶段 6：人工交接闭环与 Chatwoot 可选适配器

### 交接包

新增 `HandoffPackageBuilder`，生成不可变 JSON snapshot：

- tenant、产品/变体、硬件/固件、地区、语言；
- 用户问题摘要与原始对话；
- 附件及 OCR 结果；
- 错误码；
- retrieval evidence、被拒绝证据和引用；
- 已执行 flow/version/node/step/result；
- 转人工原因和风险等级；
- 用户联系方式、授权时间和渠道；
- traceId/requestId。

禁止让前端提交 summary 作为最终 packageSnapshot；summary 只可作为用户补充。

### 内部队列

- 状态：`NEW, ASSIGNED, IN_PROGRESS, WAITING_USER, WAITING_PARTS, RESOLVED, CLOSED, FAILED_DELIVERY`。
- 支持领取、转派、备注、优先级、SLA、关闭结果。
- 关闭时可以生成 `knowledge_candidate`，但不能自动发布。

### Chatwoot Adapter

定义：

```java
public interface HumanSupportAdapter {
    DeliveryResult create(HandoffPackage pkg);
    DeliveryResult appendMessage(...);
    void handleWebhook(...);
}
```

实现 `InternalQueueAdapter` 和可选 `ChatwootAdapter`：

- 创建/关联 contact；
- API Inbox 创建 conversation；
- 将交接摘要和必要附件发送为消息；
- 保存 externalConversationId；
- 校验 Chatwoot webhook 签名；
- 状态/人工消息回写；
- 幂等与重试，不能重复建单。

### 验收

人工打开工单即可看到型号、版本、图片、已做步骤和依据；交接失败可重试；人工结果能回流为待审核知识候选。

---

## 阶段 7：统计、评测和可观测性

### 业务事件

实现 `OperationalEventService`，事件至少包括：

- QR_RESOLVED/FAILED；
- CONVERSATION_STARTED/CLOSED；
- MESSAGE_RECEIVED；
- ATTACHMENT_PROCESSED/FAILED；
- RETRIEVAL_COMPLETED/NO_EVIDENCE/CONFLICT；
- ANSWER_COMPLETED/FAILED/REFUSED；
- FLOW_STARTED/STEP/COMPLETED/ESCALATED；
- HANDOFF_CREATED/DELIVERED/CLOSED；
- FEEDBACK_SUBMITTED。

attributes 只能保存非敏感结构化字段。

### 看板

实现：

- 会话数、扫码成功率；
- AI 自助解决率；
- 无答案率；
- 自动/主动转人工率；
- 平均响应耗时和对话轮次；
- 产品/型号/地区/语言/固件问题分布；
- 高频错误码；
- 知识使用、差评和缺口；
- 流程完成率和失败节点；
- 模型调用量、Token 和成本估算。

### 评测

实现 `EvaluationCase, EvaluationDataset, EvaluationRun, EvaluationResult`：

- 正确证据；
- 期望回答要点；
- 期望意图；
- 是否应拒答/转人工；
- 适用产品切片；
- 风险等级。

初始数据集至少：

- 50 条正常知识问题；
- 20 条相似型号/错误固件/错误地区反例；
- 15 条无答案；
- 15 条安全和 prompt injection；
- 每个 flow 主要分支。

评价项：retrieval recall、citation correctness、groundedness、correct refusal、safety recall、flow transition accuracy、latency。

### Langfuse

在 `langfuse` profile 下记录：session=conversationId、trace=answerId、retrieval span、rerank span、LLM generation、flow decision、scores。Langfuse 不可用时只记录本地 trace，不影响回答。

---

## 阶段 8：部署加固和真实 E2E

1. Compose 固定镜像和 healthcheck；API 依赖必须等待 ready，不只依赖容器启动。
2. MinIO 使用独立 app user/policy，不直接使用 root credentials。
3. 增加数据库、MinIO、Qdrant 备份恢复脚本和文档。
4. 增加数据保留、会话/附件删除任务。
5. Nginx：HTTPS 示例、CSP、HSTS、`X-Content-Type-Options`、上传限制、SSE 关闭 buffering。
6. 增加 Testcontainers：PostgreSQL；WireMock：LLM/OCR/Embedding/Rerank；可选 Qdrant/MinIO 集成测试。
7. 前端增加 Vitest/React Testing Library 和 Playwright。
8. Playwright 最终场景：
   - 管理员登录；
   - 创建产品变体和 QR；
   - 上传并发布知识；
   - 检索验证；
   - 扫码建立会话；
   - FAQ 回答带引用；
   - 图片 OCR 确认；
   - 完成诊断成功路径；
   - 触发安全停止；
   - 无答案转人工；
   - 人工领取关闭；
   - 生成知识候选；
   - 看板和评测可查看。
9. 故障演练：关闭 LLM、Qdrant、OCR、Rerank、MinIO、Chatwoot，确认降级符合设计。

---

## 6. API 目标契约

保持 REST 风格并生成 OpenAPI。建议主要接口：

```text
POST   /public/qr/resolve
POST   /public/conversations
GET    /public/conversations/{id}
POST   /public/conversations/{id}/messages
POST   /public/conversations/{id}/attachments
GET    /public/conversations/{id}/answers/stream
POST   /public/conversations/{id}/feedback
POST   /public/handoffs

GET    /api/products
POST   /api/products
GET    /api/product-variants
POST   /api/product-variants
GET    /api/qr-bindings
POST   /api/qr-bindings
POST   /api/qr-bindings/{id}/revoke

GET    /api/documents
POST   /api/documents
POST   /api/documents/{id}/revisions
GET    /api/knowledge-revisions/{id}/preview
POST   /api/knowledge-revisions/{id}/submit
POST   /api/knowledge-revisions/{id}/approve
POST   /api/knowledge-revisions/{id}/publish
POST   /api/knowledge-revisions/{id}/deprecate
POST   /api/retrieval/test

GET    /api/model-configurations
POST   /api/model-configurations
POST   /api/model-configurations/{id}/test
PUT    /api/model-configurations/{id}

GET    /api/flows
POST   /api/flows
POST   /api/flows/{id}/versions
POST   /api/flow-versions/{id}/simulate
POST   /api/flow-versions/{id}/publish

GET    /api/handoffs
POST   /api/handoffs/{id}/claim
POST   /api/handoffs/{id}/notes
POST   /api/handoffs/{id}/close

GET    /api/analytics/overview
GET    /api/analytics/issues
GET    /api/evaluation-datasets
POST   /api/evaluation-runs
GET    /api/evaluation-runs/{id}
```

公共会话 token 继续只保存 hash。所有公共会话接口都必须验证 token、会话状态和限流。

---

## 7. 测试最低要求

### 后端

- Domain state transition 单测。
- Repository tenant isolation 测试。
- QR token、会话 token、文件签名、安全规则测试。
- Retrieval：exact/FTS/vector/RRF/rerank/threshold/conflict 测试。
- Orchestrator：有证据、无证据、冲突、模型失败、非法 citation 测试。
- Flow：版本固定、所有分支、循环、风险停止测试。
- Handoff：snapshot、幂等、重试、webhook 签名测试。
- Flyway 从空库完整迁移测试。

### 前端

- API client/token 处理。
- 产品、QR、文档发布表单。
- 对话发送、SSE、断线、附件失败。
- 诊断按钮和 UNKNOWN。
- 转人工授权。
- 关键 Playwright E2E。

### 安全反例

- 跨 tenant UUID 猜测。
- 跨产品知识召回。
- 未发布/已下架知识。
- Prompt injection 要求忽略型号和系统规则。
- 上传伪造 MIME、超大图片、压缩炸弹。
- 重放二维码/会话 token。
- 重复提交 handoff。
- 用户在高风险节点诱导继续操作。

---

## 8. 编码规则

1. 不新增巨型 Controller/Service；编排和领域逻辑分开。
2. 使用构造器注入，避免 field injection。
3. 所有 tenant 查询显式带 tenantId，禁止先 `findById` 再在 Controller 判断的扩散模式。
4. 关键状态变化使用领域方法，禁止任意 setter。
5. 外部 Provider 均通过接口，可用 WireMock 测试。
6. 不记录 API Key、Token、完整附件、完整联系方式和完整用户消息正文。
7. 数据库迁移只新增新版本，禁止修改已经提交的 V1–V9。
8. 不用静态检查代替真实构建和真实测试。
9. 不创建空壳“已完成”页面；每个页面必须调用真实 API。
10. 所有重要行为必须具备 requestId、conversationId、answerId/traceId。
11. 不为了追求“AI 化”把确定性安全和状态机交给模型。
12. 首次 MVP 不实现语音、电话、自动退款、远程控制、自动刷机和自动拆机指导。

---

## 9. 每阶段的汇报格式

每完成一个阶段，输出：

```text
阶段名称：
已修改文件：
数据库迁移：
实现的业务闭环：
新增测试：
执行的命令与结果：
真实集成验证：
已知限制：
下一阶段建议：
```

不得只说“已经完成”。必须给出具体文件、测试数量、命令输出摘要和未解决风险。

---

## 10. 本轮执行范围

首次收到本指令时：

1. 完成全仓代码审计和 checklist。
2. 只实施“阶段 0：工程基线、安全清理和可复现构建”。
3. 同时为阶段 1–8 建立明确 TODO，但不要跨阶段大规模开发。
4. 阶段 0 构建与测试通过后停止，汇报结果，等待下一条指令。

不要在阶段 0 顺手重写 Retrieval、Flow 或 UI；先建立可靠基线。
