# 海外硬件 AI 智能客服平台 · 完整 UI/UX 方案

> 版本：v1.0 · 2026-07-28
> 范围：终端用户支持页（Web/PWA）+ 厂商管理后台（Console）+ 人工坐席工作台（Agent Workspace）+ 诊断流程编辑器及后端联动
> 配套：交互式设计稿 `prototype/ui-prototype.html`（本地静态服务预览）

---

## 0. 现状诊断：为什么"很基础"

通读 `repos/frontend/src/main.tsx`（单文件 235 行）与后端 7 个 Controller，定位根因：

| 维度 | 现状 | 后果 |
|---|---|---|
| 架构 | 单文件、无路由、无状态库、无 UI 库（仅 React/ReactDOM） | 无法承载多页面/多角色 |
| 录入 | 全部用 `prompt()` 弹窗 | 产品/文档/检索录入无校验、无预览、不可维护 |
| 对话 | 仅 `POST /answers` 非流式；`GET /answers/stream`(SSE) 已实现但未用 | 无打字流式、无引用卡片、无诊断进度 |
| 上下文 | 设备信息只在顶部一行 `<p>` | 用户感知弱，型号错配无提示 |
| 安全 | `Intent.SAFETY_RISK` 仅一行文本 | 无"停止操作"的强视觉阻断 |
| 人工接管 | `HandoffController` 已实现，前端**零入口** | 核心能力完全不可见 |
| 诊断流程 | 后端 `TroubleshootStateMachine`/`TroubleshootTypes` 引擎已存在，但**无实体、无仓储、无 Controller、未接入 `answers()`** | 最核心差异化能力悬空 |
| 设计语言 | Fraunces 衬线 + DM Mono + 硬阴影（编辑/粗野风） | 对多语言终端用户 + ToB 后台均显"玩具感" |

**结论**：前端目前只是后端能力的"冒烟测试页"，不是产品。

---

## 1. 设计目标与原则

**目标**：把"正确的产品知识，在正确版本和地区，对正确的问题，以安全的步骤交付给用户，并把每次人工处理沉淀为可验证知识"这件事，变成可信、可纠偏、可审计的界面体验。

**5 条设计原则（由业务难点反推）**：

1. **可信优先**——可溯源、可纠偏、可停止，比"像人"更重要（破解 RAG 幻觉）。
2. **一次一问**——AI 一次只要求一个关键步骤，再问结果（文档明确要求）。
3. **设备上下文常驻**——型号/地区/固件/错误码全程可见，错配即提示。
4. **安全边界可视化**——安全/保修/退款用差异化视觉，绝不"自然语言带过"。
5. **降级优雅**——模型/人工渠道不可用时，给已发布 FAQ 与明确告知（非无依据兜底）。

---

## 2. 标杆参考与映射

| 标杆 | 借鉴点 | 映射到本项目 |
|---|---|---|
| **Intercom / Ada**（AI Agent 原生客服） | 品牌化对话 widget、意图分流、AI→人工无缝 handoff、会话级上下文常驻 | `/support` 设备上下文常驻 + 意图分流 + 转人工 |
| **DeciZone**（受控决策树诊断） | "4 步内定位问题"、AI 由人工审核的决策树驱动、transcript 可审计 | 对应"受控诊断状态机"——流程节点可视化、不可被模型篡改 |
| **Citations / 来源引用**（Perplexity、Graphlit） | 每条回答附可点击来源 + 版本号 | 对应"有据可查"，前端渲染引用卡片而非 `chunkId` 串 |
| **坐席工作台**（Motiff AI Inbox、Zendesk Agent Workspace） | 会话列表 + 活动会话 + AI Copilot 侧栏 + 交接包 | 对应 `HandoffRequest` 队列与坐席处理 |
| **安全停机**（行业通用） | 危险操作时全屏阻断 + 明确下一步 | 对应 `Intent.SAFETY_RISK` 强阻断 UI |

---

## 3. 设计系统（Design System）

**视觉重定义**：抛弃粗野主义，采用"专业可信 · 低温清晰"风格（硬件售后 = 信任敏感场景）。

### 3.1 色彩
```
--brand:        #0F4C5C  深青蓝（信任/技术，主色）
--brand-soft:   #E6F0F2  主色浅底
--ai:           #1FB6A6  青绿（AI 行为/强调）
--human:        #E8833A  暖橙（转人工/坐席，与 AI 区分）
--ink:          #0F172A  主文字
--ink-2:        #475569  次文字
--line:         #E2E8F0  描边
--bg:           #F8FAFC  页面底
--ok:           #16A34A  成功
--warn:         #D97706  警告
--danger:       #DC2626  危险/安全停机
--info:         #2563EB  信息
```

### 3.2 排版
- 终端用户：易读无衬线 `Inter / system-ui`（多语言友好）。
- 后台数据：紧凑 `Inter` + 错误码/JSON 用等宽 `JetBrains Mono`。
- 8pt 栅格；圆角 8/12px；柔和阴影（告别硬阴影像素块）。

### 3.3 组件与占位态
统一：按钮（主/次/危险）、输入框、卡片、状态徽章（彩色流程点）、骨架屏、Toast、空状态插画、引用卡片、步骤卡、对话气泡、节点卡。

### 3.4 技术选型（与现有 React 契合）
- 路由 `react-router-dom`；状态 `zustand`；数据 `TanStack Query`（缓存 API）。
- 后台/坐席：`shadcn/ui`（Radix + Tailwind）或 Ant Design（ToB 成熟表单/表格）。
- 录入校验 `react-hook-form`（替代 `prompt()`）；转场 `framer-motion`。
- 诊断流程编辑器节点画布：自研轻量 SVG/绝对定位画布（或 `@xyflow/react`/React Flow）。

---

## 4. 信息架构（IA）

```
/support/:qrToken        → 终端用户支持（Web/PWA，无需登录）
/console/*  (登录后)     → 厂商管理后台
   ├ /overview          总览
   ├ /products          产品中心（树）
   ├ /qrs               二维码
   ├ /knowledge         知识中心（文档 / FAQ / 诊断流程）
   ├ /search            检索验证
   ├ /handoffs          人工协同队列
   ├ /analytics         运营分析
   └ /settings          模型/语言/权限
/agent/*    (坐席登录)   → 人工坐席工作台（可并入 console 的"人工协同"）
```

---

## 5. 终端用户支持页（核心，逐区域交互）

**5.1 设备识别横幅（顶部常驻）**
- 解析 `qrToken` 后展示：产品图 + 显示名 + 地区 + 固件/硬件版本 + 语言切换。
- 失败态：令牌失效/地区不符 → 内联"手动选择产品"降级卡（调用 `POST /conversations/{id}/product-context`），保留原始扫码上下文。
- 错配提示：用户改选型号后，旧诊断步骤**明确标注已结束**（对应后端 close 旧 context）。

**5.2 对话流（中间主区）**
- 消息气泡：用户右、AI 左；AI 气泡下方挂**引用卡片**（来源标题 + 知识版本，点击展开），取代当前 `来源：xxx,xxx`。
- **流式输出**：切到 `GET /answers/stream`(SSE)，打字机 + "正在分析"骨架。
- **一次一问**：AI 提问以"步骤卡"呈现（编号 + 操作 + 预期现象 + 确认按钮「已解决/未解决/不清楚」），不是大段文字。
- 错误码：输入框上方"或输入错误码"快捷入口（正则 `^\w[\w-]{2,}$`）。
- 图片：上传前弹**隐私引导**（"请勿上传身份证/银行卡"），上传后显示缩略图，失败不丢文字。

**5.3 诊断进度侧栏（右侧，桌面端）**
- 可视化当前 `TroubleshootFlow` 节点树：已完成/当前/未达，用户一眼看到"走到第几步"。
- 安全节点红点；强制人工节点橙点。

**5.4 安全停机（阻断式）**
- 命中 `Intent.SAFETY_RISK` → 全宽红条 + 图标 + "请立即停止使用，联系人工"，禁用后续操作输入，仅留"转人工"。

**5.5 转人工交接包**
- 触发：用户主动 / 系统自动（证据不足、连续失败、保修退款）。
- 表单：最小联系方式 + 同意授权（对应 `contactAuthorized`），显示渠道与时区说明。
- 提交后展示**请求编号**，提示"无需重复提交"（调用 `POST /public/handoffs`，幂等 `idempotencyKey`）。

**5.6 反馈**
- 对话末"本次是否解决？"二态反馈（对应 `POST /feedback`）；解决→感谢，未解决→引导转人工。

---

## 6. 厂商管理后台（Console）逐模块

**6.1 总览 Dashboard**：卡片（今日扫码/会话/自主解决率/转人工/待审核/风险拦截）+ 趋势迷你图 + Top 未解决问题。

**6.2 产品中心（树）**：`Tenant→Brand→Family→Model→HW Rev→Firmware→Region`；左树右详情（别名、图片、配件、固件区间、停售、二维码批次）。结构化表单替代 `prompt()`。

**6.3 二维码管理**：列表（批次/序列号/状态/过期）；创建弹窗；**令牌仅显示一次**用一次性模态 + 复制按钮；撤销带原因。

**6.4 知识中心（三栏：文档 / FAQ / 诊断流程）**
- 文档：`UPLOADED→DRAFT→REVIEW→APPROVED→PUBLISHED→DEPRECATED/ARCHIVED` 用**彩色流程条**可视化；预览用分块列表 + 原文对照。
- FAQ 编辑器：单点事实 CRUD。
- **诊断流程编辑器**：可视化节点画布（question/operation/decision/handoff/end 五类），配置适用范围、分支、预期、停止条件、风险级别、来源关联；"模拟会话"按钮（见第 9 节）。

**6.5 检索验证**：表单（查询+产品+地区+语言）→ 结果展示**命中块 + 适用范围 + 来源**，供专家核验过滤是否正确。

**6.6 人工协同队列**：列表（待处理/处理中/已关闭 + 原因 + 优先级）→ 交接包详情（型号、对话、已尝试步骤、图片、错误码、AI 引用、转人工原因）→ 坐席 claim/close + 结果标签 + 提议知识。

**6.7 运营分析**：意图分布、问题分类、错误码分布、未解决 TopN、无答案 TopN、被差评 TopN、知识使用/过期/冲突、平均响应时长（对应文档 3.5 P0 看板）。

**6.8 设置**：模型供应商/密钥（脱敏）、语言/术语库、角色权限（ADMIN / KNOWLEDGE_REVIEWER / 坐席 / 只读）。

---

## 7. 人工坐席工作台（Agent Workspace）

三栏：**交接队列**（左）· **活动会话 + 交接包**（中）· **AI Copilot 侧栏**（右）。
- Copilot：根据对话自动建议回复草稿、调取相关知识、标注知识缺口；坐席一键采纳/改写。
- 关闭时必选结果标签（已解决/待配件/保修/放弃/缺陷）+ 可"提议新增/修订知识"（回流为知识草稿，不直接污染正式知识）。

---

## 8. 后端 TroubleshootFlow 接口设计（新增，复用现有引擎）

> 现状：`troubleshoot/TroubleshootStateMachine.java` 与 `TroubleshootTypes.java` 已定义枚举与分支/安全升级逻辑，但无持久化与对外服务。以下设计在不变更引擎语义的前提下补齐实体、仓储、Controller 与运行时接入。

### 8.1 实体（沿用 JPA + 不可变构造 + 生命周期方法模式，对齐 `KnowledgeRevision`）

```java
// TroubleshootFlow.java
@Entity @Table(name = "troubleshoot_flows")
class TroubleshootFlow {
  @Id UUID id;
  @Column(name="tenant_id") UUID tenantId;
  String title;
  @Enumerated(EnumType.STRING) Intent triggerIntent;   // 触发意图，如 TROUBLESHOOTING/ERROR_CODE
  @Column(name="product_model_id") UUID productModelId; // 适用范围
  String region; String locale;
  @Column(name="firmware_min") String firmwareMin;
  @Column(name="firmware_max") String firmwareMax;
  @Enumerated(EnumType.STRING) Status status;
  String owner; @Column(name="published_at") Instant publishedAt;
  // 生命周期：DRAFT→REVIEW→APPROVED→PUBLISHED→DEPRECATED→ARCHIVED
  void submit(){ if(status!=DRAFT) throw ...; status=REVIEW; }
  void approve(UUID u){ ... status=APPROVED; }
  void publish(UUID u){ require applicability; status=PUBLISHED; publishedAt=now; }
  void deprecate(){ ... }
  void restore(UUID u){ ... }
  enum Status { DRAFT, REVIEW, APPROVED, PUBLISHED, DEPRECATED, ARCHIVED }
}

// TroubleshootNode.java
@Entity @Table(name = "troubleshoot_nodes")
class TroubleshootNode {
  @Id UUID id;
  @Column(name="flow_id") UUID flowId;
  @Column(name="node_key") String nodeKey;        // 流程内唯一，如 wifi-check-01
  @Enumerated(EnumType.STRING) NodeType nodeType; // QUESTION/OPERATION/DECISION/HUMAN_ESCALATION/END
  @Column(columnDefinition="text") String prompt; // 自然语言提示（可由模型翻译，但跳转不可改）
  @Enumerated(EnumType.STRING) Risk risk;         // LOW/MEDIUM/HIGH
  String expectedInput;                            // yes_no_unknown / free_text / code
  @Column(name="branch_yes") String branchYes;
  @Column(name="branch_no") String branchNo;
  @Column(name="branch_unknown") String branchUnknown;
  @Column(name="branch_next") String branchNext;  // OPERATION 顺序下一节点
  boolean safetyStop;                             // 强制停机节点
  @ElementCollection List<String> sourceRefs;     // 关联 KB-xxx / MANUAL-xxx
  int orderIndex;
}
```

### 8.2 仓储（`TroubleshootRepositories.java`，沿用聚合风格）

```java
interface TroubleshootFlowRepository extends JpaRepository<TroubleshootFlow, UUID> {
  List<TroubleshootFlow> findAllByTenantIdOrderByCreatedAtDesc(UUID t);
  Optional<TroubleshootFlow> findByIdAndTenantId(UUID id, UUID t);
  Optional<TroubleshootFlow> findPublishedMatch(UUID tenant, UUID product, String region,
      String locale, Intent trigger); // 运行时按适用范围取唯一已发布流程
}
interface TroubleshootNodeRepository extends JpaRepository<TroubleshootNode, UUID> {
  List<TroubleshootNode> findAllByFlowIdOrderByOrderIndexAsc(UUID flowId);
  Optional<TroubleshootNode> findByFlowIdAndNodeKey(UUID flowId, String nodeKey);
}
```

### 8.3 Controller（`/api/flows`，权限对齐知识模块）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/flows` | ADMIN | 新建流程（DRAFT），返回 `id` |
| GET | `/api/flows` | ADMIN/REVIEWER | 租户内列表（含状态、适用范围） |
| GET | `/api/flows/{id}` | ADMIN/REVIEWER | 流程 + 节点全量（编辑器加载） |
| PUT | `/api/flows/{id}` | ADMIN | 改标题/触发意图/适用范围 |
| POST | `/api/flows/{id}/nodes` | ADMIN | 新增节点 |
| PUT | `/api/flows/{id}/nodes/{nodeKey}` | ADMIN | 改节点（prompt/risk/分支/sourceRefs/safetyStop） |
| DELETE | `/api/flows/{id}/nodes/{nodeKey}` | ADMIN | 删节点（同时清理指向它的分支） |
| POST | `/api/flows/{id}/submit` | ADMIN/REVIEWER | DRAFT→REVIEW |
| POST | `/api/flows/{id}/approve` | ADMIN/REVIEWER | REVIEW→APPROVED |
| POST | `/api/flows/{id}/publish` | ADMIN/REVIEWER | APPROVED→PUBLISHED（强制适用范围） |
| POST | `/api/flows/{id}/deprecate` | ADMIN/REVIEWER | PUBLISHED→DEPRECATED（新会话停用） |
| POST | `/api/flows/{id}/restore` | ADMIN/REVIEWER | DEPRECATED→PUBLISHED（回滚） |
| POST | `/api/flows/{id}/simulate` | ADMIN/REVIEWER | **模拟会话**，不触达真实人工渠道 |

**`simulate` 请求/响应示例**
```json
POST /api/flows/{id}/simulate
{ "startReply": "YES", "path": ["wifi-check-02","network-reset-01"] }
→ { "transcript": [
    {"nodeKey":"wifi-check-01","prompt":"设备能搜索到其他 Wi‑Fi 吗？","expectedInput":"yes_no_unknown","risk":"LOW"},
    {"nodeKey":"wifi-check-02","prompt":"请重启路由器…","expectedInput":"free_text","risk":"LOW"},
    {"node":"END","prompt":"如已恢复请确认。","escalated":false}
  ], "escalated": false, "coverage": {"nodes":8,"visited":6,"unreachable":["image-request-01"]} }
```

### 8.4 运行时接入 `ConversationController.answers()`（联动核心）

当前 `answers()` 仅做 RAG。改造为"意图路由优先于生成"：

```
intent = intents.classify(lastUserText)
if intent == SAFETY_RISK → 返回危险停机回答 + 前端 HUMAN_ESCALATION 控制
if intent == HUMAN_REQUEST → 返回转人工提示 + 前端 HUMAN_ESCALATION 控制
if intent in {TROUBLESHOOTING, ERROR_CODE}:
    flow = flows.findPublishedMatch(tenant, productModelId, region, locale, intent)
    if flow 存在:
        node = 读会话当前节点(或流程首节点)
        reply = normalizeReply(lastUserText, node.expectedInput)  // 关键词/模型归一为 YES/NO/UNKNOWN/REFUSE
        failures = 会话连续失败计数（"未解决"反馈 +1）
        t = stateMachine.next(node.nodeType, node.risk, reply, node.branchYes, node.branchNo, node.branchUnknown, failures)
        if t.escalated → 自动创建 HandoffRequest + 返回 HUMAN_ESCALATION 控制
        else → 写会话当前节点 = t.nextNodeKey；返回该节点 prompt + expectedInput + risk + sourceRefs + flowControl(进度)
    else → 退回 RAG 路径（证据不足则转人工）
else → RAG 知识回答（带引用）
```

需在 `Conversation` 增加字段：`currentFlowId`、`currentNodeKey`、`flowFailures`；消息 `MessageView` 增加 `flowControl`（节点进度/risk/expectedInput）与 `citations` 已支持。

---

## 9. 前端诊断流程编辑器与后端联动设计

### 9.1 编辑器架构（组件树）
```
FlowEditorPage
├ FlowListPanel        // 左：流程列表（GET /api/flows）
├ Canvas               // 中：SVG 边 + 节点卡（绝对定位）
│   └ FlowNodeCard     // 5 类节点配色/图标；安全节点红边、人工节点橙边
├ Inspector            // 右：选中节点编辑（PUT /api/flows/{id}/nodes/{key}）
│   ├ NodeTypeSelect
│   ├ PromptEditor
│   ├ RiskSelect + SafetyStopToggle
│   ├ BranchEditor     // yes/no/unknown/next 下拉（仅同流程节点）
│   └ SourceRefsInput  // 关联 KB/MANUAL
└ Toolbar
    ├ SaveNode / DeleteNode
    ├ PublishFlow      // 生命周期按钮（对齐状态机流程条）
    └ SimulateButton   // → POST /api/flows/{id}/simulate
```

### 9.2 前端节点模型（与后端 1:1）
```ts
type FlowNode = {
  nodeKey: string; nodeType: 'QUESTION'|'OPERATION'|'DECISION'|'HUMAN_ESCALATION'|'END';
  prompt: string; risk: 'LOW'|'MEDIUM'|'HIGH'; expectedInput: string;
  branchYes?: string; branchNo?: string; branchUnknown?: string; branchNext?: string;
  safetyStop: boolean; sourceRefs: string[]; orderIndex: number;
};
```

### 9.3 编辑器 ↔ 后端 联动时序
1. 进入编辑器：`GET /api/flows/{id}` → 渲染画布（按 `branch*` 连线）。
2. 拖拽新增节点：`POST /api/flows/{id}/nodes` → 返回 `nodeKey` 后本地落点。
3. 右侧改属性：`PUT /api/flows/{id}/nodes/{key}`（防抖保存）。
4. 连分支：在 Inspector 选目标 `nodeKey` → `PUT` 更新 `branch*`。
5. 生命周期：工具栏按状态显示对应按钮（送审/批准/发布/下架/回滚）→ 对应 `POST` 动作；状态用彩色流程条展示。
6. **模拟**：`POST /api/flows/{id}/simulate` → 复用支持页的"步骤卡"组件，以对话式逐步走查，输出 `coverage`（未覆盖节点告警），专家确认分支覆盖后再发布。

### 9.4 运行时联动（前端侧）
- 支持页收到 `flowControl`：渲染步骤卡（按 `expectedInput` 显示按钮）、右侧进度树（按 `orderIndex`/分支高亮当前节点）、安全节点红点。
- `escalated=true`：前端切到转人工交接包（复用 5.5），并附带 `summary`（已尝试步骤来自 `flowControl` 历史）。

---

## 10. 关键交互模式清单

流式回答 · 引用卡片 · 图片隐私引导 · 错误码快捷 · 一次一问步骤卡 · 诊断进度树 · 安全停机红条 · 转人工交接包 · 状态机流程条 · 骨架屏/Toast/空状态 · 多语言切换 · 移动端 PWA（对话页响应式优先）。

---

## 11. 实施路线（结合现有代码，不重写）

**技术地基**：引入 `react-router-dom` + `zustand` + `TanStack Query` + `Tailwind` + `shadcn/ui`，把 `main.tsx` 拆为 `src/{support,console,agent}/{pages,components}`。

**P0（先把已实现的做"对"）**
1. 支持页重做：设备横幅 + 流式 + 引用卡片 + 一次一问 + 安全停机 + 转人工（接 `HandoffController`）。
2. Console 4 个 Tab 重构：结构化表单替代 prompt，知识状态机流程条，检索结果可视化。
3. 坐席/人工协同入口（接 `HandoffController` list/claim/close）。

**P1（补齐业务闭环）**
4. 诊断流程后端（第 8 节）+ 前端编辑器（第 9 节）+ `answers()` 运行时接入。
5. FAQ 编辑器（后端补简单 CRUD）。
6. 运营分析 Dashboard、设置/权限/模型配置。

---

## 12. UI 层验收清单（来自 MVP 验收）

- 设备识别可见且可纠偏；一次一问。
- 引用可点可溯源（来源标题 + 知识版本）。
- 安全停机强阻断（红条 + 仅转人工）。
- 转人工交接包字段完整（型号/对话/已尝试/图片/错误码/AI 引用/原因）。
- 流式无卡死，模型/人工不可用时优雅降级。
- 诊断流程编辑器可保存/模拟/发布，模拟覆盖未达节点告警。
- 全模块空状态/降级提示；移动端对话可用。

---

## 13. 设计稿

交互式高保真设计稿见 `prototype/ui-prototype.html`，本地静态服务预览，包含：终端支持页、管理后台总览、知识中心、诊断流程编辑器（可点选节点 + 模拟走查）、人工坐席工作台。

---

## 14. P1 实施规格：诊断流程后端 + 编辑器 + 运行时接入

> 目标：复用已有 `TroubleshootStateMachine` / `TroubleshootTypes`，补齐实体、仓储、Controller 与 `answers()` 接入，并落地前端编辑器与对话步骤卡。

### 14.1 后端新增文件（`repos/backend/.../troubleshoot/`）

| 文件 | 内容 |
|---|---|
| `TroubleshootFlow.java` | JPA 实体：id, tenantId, title, `Intent triggerIntent`, productModelId, region, locale, firmwareMin, firmwareMax, `Status status`, owner, publishedAt。生命周期方法 `submit/approve(UUID)/publish(UUID)/deprecate/restore(UUID)`，状态 `DRAFT→REVIEW→APPROVED→PUBLISHED→DEPRECATED→ARCHIVED`（对齐 `KnowledgeRevision`）。 |
| `TroubleshootNode.java` | JPA 实体：id, flowId, `nodeKey`（流程内唯一）, `NodeType nodeType`（复用枚举）, `Risk risk`（复用枚举）, prompt(text), expectedInput(`yes_no_unknown`/`free_text`/`code`), branchYes/branchNo/branchUnknown/branchNext(String), safetyStop(bool), `List<String> sourceRefs`(ElementCollection), orderIndex。 |
| `TroubleshootRepositories.java` | `TroubleshootFlowRepository`、`TroubleshootNodeRepository`（含 `findAllByTenantIdOrderByCreatedAtDesc`、`findByIdAndTenantId`、`findPublishedMatch(tenant,product,region,locale,trigger,firmware)`、`findAllByFlowIdOrderByOrderIndexAsc`、`findByFlowIdAndNodeKey`、`deleteByFlowIdAndNodeKey`）。 |
| `TroubleshootController.java` | `@RequestMapping("/api/flows")`，见 14.2。 |

**`Conversation` 实体扩展**：新增 `currentFlowId UUID`、`currentNodeKey String`、`flowFailures int`（驱动对话内流程状态）。若 `spring.jpa.hibernate.ddl-auto=update` 则自动建表，否则需补迁移。

### 14.2 Controller 端点（`/api/flows`，权限对齐知识模块）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/flows` | ADMIN | 新建流程（DRAFT），返回 `FlowView(id,status)` |
| GET | `/api/flows` | ADMIN/REVIEWER | 租户内列表（含状态/适用范围） |
| GET | `/api/flows/{id}` | ADMIN/REVIEWER | 流程 meta + 节点全量（编辑器加载） |
| PUT | `/api/flows/{id}` | ADMIN | 改标题/触发意图/适用范围 |
| POST | `/api/flows/{id}/nodes` | ADMIN | 新增节点（校验 nodeKey 唯一） |
| PUT | `/api/flows/{id}/nodes/{nodeKey}` | ADMIN | 改节点（含分支/risk/safetyStop/sourceRefs） |
| DELETE | `/api/flows/{id}/nodes/{nodeKey}` | ADMIN | 删节点并清理指向它的分支 |
| POST | `/api/flows/{id}/submit` | ADMIN/REVIEWER | DRAFT→REVIEW |
| POST | `/api/flows/{id}/approve` | ADMIN/REVIEWER | REVIEW→APPROVED |
| POST | `/api/flows/{id}/publish` | ADMIN/REVIEWER | APPROVED→PUBLISHED（强制适用范围） |
| POST | `/api/flows/{id}/deprecate` | ADMIN/REVIEWER | PUBLISHED→DEPRECATED |
| POST | `/api/flows/{id}/restore` | ADMIN/REVIEWER | DEPRECATED→PUBLISHED（回滚） |
| POST | `/api/flows/{id}/simulate` | ADMIN/REVIEWER | 模拟走查，返回 transcript + coverage |

**`simulate` 契约**
```json
POST /api/flows/{id}/simulate   →
{ "transcript":[ {"nodeKey","nodeType","prompt","expectedInput","risk","escalated"} … ],
  "escalated": false,
  "coverage": { "nodes":8, "visited":6, "unreachable":["image-request-01"] } }
```
实现：从首节点做分支图可达性遍历得 `coverage`；`transcript` 取一条代表路径（优先 yes/next）直到 END 或 escalation。

### 14.3 运行时接入 `ConversationController.answers()`

保持"意图路由优先于生成"；复用已有 `TroubleshootStateMachine.next(type,risk,reply,yes,no,unknown,failures)`。

```
intent = intents.classify(lastUserText)
if SAFETY_RISK / HUMAN_REQUEST → 现有处理（HUMAN_REQUEST 仍走转人工）
if intent in {TROUBLESHOOTING, ERROR_CODE}:
    flow = flows.findPublishedMatch(tenant, productModelId, region, locale, intent)  // 取适用范围命中的已发布流程
    if flow != null:
        if conversation.currentNodeKey == null:
            node = 首节点(flow)                       // 用户首条消息是问题描述，不解析为回复
        else:
            node = nodeByKey(flow, currentNodeKey)
            reply = normalize(lastUserText, node)      // yes_no_unknown→YES/NO/UNKNOWN/REFUSE；free_text→已解决=YES(失败计数不增)/未解决=NO(失败+1)
            t = stateMachine.next(node.nodeType, node.risk, reply, node.branchYes, node.branchNo, node.branchUnknown, conversation.flowFailures)
            if t.escalated: 自动创建 HandoffRequest；conversation.currentNodeKey=null；返回 escalation 回答(含 handoffId)
            else: next = nodeByKey(flow, t.nextNodeKey); conversation.currentNodeKey = next.key; conversation.flowFailures = (未解决?+1:0)
        return Answer(intent=TROUBLESHOOTING, content=node.prompt, citationChunkIds=node.sourceRefs,
                      expectedInput=node.expectedInput, risk=node.risk.name(),
                      flowControl={flowId, nodeKey, nodeType, path, totalSteps, end=(END), escalated=false})
    else: 退回 RAG（证据不足按现有逻辑转人工）
else: RAG（带引用）
```
说明：OPERATION 节点的 branchYes/branchNo 均设为 `branchNext`；"未解决"使 `flowFailures+1`，`stateMachine` 在 `failures>=2` 或 `risk==HIGH` 或 `REFUSE` 时返回 escalation。固件区间匹配为可选（上下文无固件时跳过）。

### 14.4 前端新增/修改

**新增 `src/console/FlowsPage.tsx`（诊断流程编辑器）**
- 左：流程列表（GET /api/flows）；中：自动布局画布（节点来自 GET /api/flows/{id}，按分支图分层定位，SVG 连线 branchYes=绿/branchNo=红/branchUnknown=橙/next=灰）；右：Inspector（nodeType/prompt/risk/safetyStop/分支下拉(同流程节点)/sourceRefs）。
- 操作：新建流程、新建节点(POST)、保存节点(PUT)、删除(DELETE)、生命周期按钮（送审/批准/发布/下架/回滚）、**模拟会话**（POST simulate → 步骤走查弹窗 + unreachable 告警）。
- 自动布局基于分支图，无需后端存坐标（手动拖拽为本地增强，不在 P1）。

**修改 `src/pages/SupportPage.tsx`**
- `Answer` 类型扩展 `expectedInput?`、`risk?`、`flowControl?`。
- 当回答含 `flowControl` 且非 escalation → 渲染 `StepCard`（步骤号 + prompt + 按 expectedInput 渲染按钮：yes/no/unknown 或 已解决/未解决，或自由文本输入），按钮点击即发送该回复触发下一步。
- 右侧进度侧栏 `FlowProgress`：依据 `flowControl.path` 渲染已访问节点链（当前高亮、安全节点红点）。
- escalation（flowControl.escalated 或 intent=HUMAN_ESCALATION）→ 复用现有安全停机红条 + 转人工（交接包已自动创建）。

**修改 `src/lib/types.ts` / `api.ts`**：补充 `FlowView`、`NodeView`、`SimulateResponse` 类型与 `/api/flows` 调用封装。

### 14.5 实施顺序（建议）
1. 后端实体 + 仓储 + Controller（CRUD/生命周期/simulate），`Conversation` 扩展字段。
2. `answers()` 运行时接入（意图分流 + 状态机 + 自动转人工 + flowControl 返回）。
3. 前端 `FlowsPage` 编辑器（含模拟）。
4. 前端 `SupportPage` 步骤卡 + 进度侧栏。
5. 类型检查 + 构建 + 联调（后端需跑起来；用已有二维码令牌进入 `/support/:token` 走通"提问→步骤→解决/转人工"）。

### 14.6 验收
- 专家可在编辑器新建/编辑/模拟/发布流程；simulate 报告未覆盖节点。
- 终端用户扫码后进入故障流程，AI 一次只问一个关键步骤；连续两次未解决或安全风险自动转人工并附交接包。
- 模型不能跳过安全节点或篡改分支（跳转由后端状态机决定）。
- 流程下架后新会话不再使用该版本。
