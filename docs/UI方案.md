# UI 方案：多语言 AI 售后诊断与客服平台

> 本文档为前端 UI 专项的设计系统基线（Design System Spec）与页面改版方向。
> 覆盖：`repos/frontend`（React 19 + Vite + Tailwind + react-i18next）。
> 目标：将现有"可用但通用"的后台/终端界面升级为符合产品定位的、具有辨识度与信任感的精密控制台风格。

---

## 0. 文档状态

- 现状：前端已实现完整功能（终端对话 `SupportPage`、运营管理后台 8 个页面），但视觉为通用企业 SaaS 模板风，缺乏产品辨识度、层级与氛围。
- 本文档定位：UI 专项的**设计基线 + 改版路线图**，是后续所有界面改动的唯一视觉依据。
- 约束：所有文案必须走 `t()`（i18n）；组件对外签名尽量向后兼容；不引入重型运行时依赖（动效用纯 CSS）。

---

## 1. 定位分析（为什么是这样，而不是那样）

### 1.1 产品本质

本平台服务**出海硬件厂商**，面向两类受众：

| 受众 | 场景 | 情绪与诉求 |
| --- | --- | --- |
| 终端用户（扫码进来的真实客户） | 设备坏了，扫码求助 | 焦虑、急于解决；需要**被信任的、清晰的、安全的**指引；担心拆机/触电/数据丢失风险 |
| 运营/客服人员（管理后台） | 管产品、管知识、处理人工转接 | 高效、密度、可追溯；像在操作一台**精密仪器** |

### 1.2 当前 UI 的核心问题

1. **无辨识度**：字体用 `Inter` + 系统字体（典型的"AI 生成风"默认字体）；配色是平铺的 slate + 一抹 teal，与任意后台无差异。
2. **无层级与氛围**：页面背景纯白/浅灰；卡片、表格、弹窗视觉权重相同；没有"哪里重要""当前在哪一步"的引导。
3. **核心隐喻缺失**：本产品最强差异化是**结构化故障诊断流程（确定性状态机、分支、安全停止）**，但 `SupportPage` 的流程卡片只是一个普通边框盒子，诊断"进度感""分支感""风险感"完全没表达出来。
4. **AI ↔ Human 二元关系未视觉化**：AI 回答、人工转接、安全停止三类消息视觉几乎一致，用户分不清"现在是 AI 在排障"还是"已转人工"。
5. **零动效**：没有加载、流式回答、状态切换的反馈，体验偏"死"。
6. **缺乏空状态与插画**：`EmptyState` 只有一段文字。

### 1.3 设计目标（Design Goals）

- **信任感优先（Trust First）**：售后涉及安全，界面必须沉稳、精确、可预期，不花哨。
- **诊断可视化（Diagnosis Made Visible）**：把"状态机/分支/安全停止"变成用户能看懂的进度与信号。
- **二元清晰（AI vs Human）**：teal=AI 智能，orange=人工接管，red=安全停止，形成稳定视觉语言。
- **精密仪器感（Precision Instrument）**：像工业 HMI / 航空仪表盘——克制的留白、清晰的数据排版、Mono 字体承载"代码/型号/编号"。
- **国际可达（Globally Legible）**：字号、对比度、间距对多语言（含 CJK、德法西）友好；不依赖特定文化符号。

---

## 2. 风格定位（Aesthetic Direction）

> **方向代号：Precision Instrument Console（精密仪表控制台）**
> 气质：沉稳、精确、可信、现代；带一丝"工程/工业"的硬朗，而非消费品式的圆润可爱。

- **不是**消费级可爱风、不是紫渐变 AI 风、不是极简到空。
- **是**：冷静的 teal 主色 + 暖橙人工信号 + 精密灰阶 + Mono 数据字体 + 蓝图点阵底纹 + 克制的动效。

### 2.1 三条视觉主线

| 主线 | 表达 | 落到界面 |
| --- | --- | --- |
| AI 智能（teal） | 智能、自动、流式 | AI 回答气泡、诊断进度、加载脉冲 |
| 人工接管（orange） | 人、温度、责任 | 转人工按钮、工单、处理人 |
| 安全停止（red） | 风险、断电/拆机/触电 | HIGH 风险步骤、SAFETY_STOP 消息、风险徽标 |

---

## 3. 设计系统（Design Tokens）

### 3.1 字体（Typography）

弃用通用 `Inter`，采用工程气质 trio（通过 `@fontsource` 自托管，避免外链 CDN / GDPR 风险）：

| 角色 | 字体 | 用途 |
| --- | --- | --- |
| Display / 标题 | **Sora** | h1–h3、品牌字、大数字 |
| Body / 正文 | **Manrope** | 正文、按钮、表单 |
| Mono / 数据 | **JetBrains Mono**（沿用） | 型号、会话号、错误码、流程节点 ID |

字号阶梯（Tailwind `fontSize` 扩展）：`xs 12 / sm 13 / base 14 / md 15 / lg 17 / xl 20 / 2xl 26 / 3xl 34`。
行高：正文 `1.6`，标题 `1.2`。

### 3.2 色彩（Color）

保留品牌 teal 核心，扩展为有意层级的系统；用 CSS 变量统一管理。

```
brand（主色，深青）
  900 #07323C   800 #0B3F4C   700 #0F4C5C*  600 #14647A   500 #1B7E97
  100 #D6E7EC    50  #EAF2F4   soft #E6F0F2*

ai（AI 信号，青绿）   600 #0E9D8F   500 #1FB6A6*   100 #D7F5F1
human（人工信号，暖橙）600 #C96A1F  500 #E8833A*   100 #FDEBDD

ink（文字）  #0F172A    ink2（次级）#475569   ink3（弱）#94A3B8
line（描边） #E2E8F0
panel（卡面）#FFFFFF    canvas（页面底）#F5F8F9

ok #16A34A   warn #D97706   danger #DC2626   info #2563EB
safety #E11D48   (HIGH 风险专属，比 danger 更醒目，用于安全停止)
```

语义背景（tag/banner 用）：`ok.bg #DCFCE7`、`warn.bg #FEF3C7`、`danger.bg #FEE2E2`、`info.bg #DBEAFE`、`human.bg #FDEBDD`、`safety.bg #FFE4E6`。

### 3.3 圆角 / 阴影 / 间距

- 圆角：`sm 8 / md 12 / lg 16 / xl 20 / full 999`。比现有略大，更现代。
- 阴影（层级）：
  - `shadow-xs` `0 1px 2px rgba(15,23,42,.04)`
  - `shadow-card` `0 1px 2px rgba(15,23,42,.06), 0 8px 24px rgba(15,23,42,.06)`（现状沿用）
  - `shadow-pop` `0 12px 32px rgba(15,23,42,.12)`（弹层/抽屉）
  - `shadow-inset` `inset 0 1px 0 rgba(255,255,255,.6)`
- 间距：沿用 Tailwind 默认 4px 栅格；页面主区 `p-6`→`p-8`，卡片内距 `p-4`→`p-5`。

### 3.4 背景与氛围（Atmosphere）

- 页面底 `canvas` 叠加极淡**蓝图点阵**（radial-gradient 点，`--dot 1px`），密度低、不干扰阅读。
- 顶栏/侧栏玻璃感：`backdrop-blur` + 半透明，制造层次。
- 空状态/登录 Hero：低饱和**渐变网格（gradient mesh）** + 点阵，营造"在诊断/连接"的氛围。

### 3.5 动效（Motion，纯 CSS keyframes）

| 名称 | 用途 |
| --- | --- |
| `fade-in` / `slide-up` | 页面与卡片入场（错峰 `animation-delay`） |
| `pop-in` | 弹窗/抽屉出现 |
| `pulse-ring` | AI 思考中（流式回答时的脉冲环） |
| `typing` | 流式回答打字光标 |
| `shimmer` | 骨架/加载占位 |
| `step-in` | 诊断步骤卡滑入 |
| `grid-pan` | 背景点阵缓慢漂移（氛围） |

原则：单次关键动效（页面载入错峰）> 零散微交互；动效时长 150–300ms，缓动 `cubic-bezier(.2,.7,.3,1)`。

---

## 4. 组件库升级（Component Library v2）

位置：`src/components/ui.tsx`（重构）+ 新增文件。所有组件支持 `className` 合并（clsx）。

### 4.1 重构现有

- **Button**：内置 `loading` 态（spinner 替代文字）；`icon` 插槽；`as` 支持 `a`；变体保留 `default/primary/ai/danger/ghost` 并微调配色与圆角。
- **Card / Panel**：新增 `elevation`（`flat/card/pop`）与 `interactive`（hover 微抬升）。
- **Tag / Badge**：新增左侧 `dot` 指示（状态点），`tone` 复用语义色；`size` sm/md。
- **Input / Textarea / Select**：统一 `Field` 包裹（label + 控件 + hint + error）；聚焦环用 `brand` 而非 `slate`。
- **Modal**：`pop-in` 动画 + 焦点陷阱 + `Esc` 关闭 + 滚动锁定；标题区加图标位。
- **EmptyState**：插画位（内联 SVG 图标）+ 主操作按钮位，不再只有文字。

### 4.2 新增组件

| 组件 | 用途 | 关键形态 |
| --- | --- | --- |
| `Sidebar` | 后台导航壳 | 品牌块（渐变+字标）、分组导航、激活态"竖条"指示、可折叠、底部语言+用户 |
| `TopBar` / `PageHeader` | 顶栏/页头 | 面包屑 + 标题 + 副标题 + 右侧操作区；全局健康状态点 |
| `StatCard` | 指标卡 | 标签 + 大数字（Sora）+ 趋势 delta + 可选迷你 spark（CSS） |
| `Banner` | 通知/安全提示 | `tone`（info/warn/danger/safety）；可关闭；用于 SAFETY_STOP、系统通知 |
| `Toast` | 轻提示 | 成功/错误/信息；右上角堆叠；自动消失 |
| `Drawer` | 详情抽屉 | 右侧滑入；用于工单详情、产品变体、流程节点 |
| `Stepper` / `DiagnosisRail` | 诊断进度 | 纵向步骤条 + 连接线 + 当前节点脉冲 + 风险节点红标 |
| `Tooltip` | 提示 | 轻量 hover 提示 |
| `Tabs` / `Segmented` | 分段切换 | 用于对话分支选择（YES/NO/UNKNOWN） |
| `Avatar` | 处理人 | 首字母 + 颜色 |
| `DataTable` | 表格封装 | 粘性表头、行 hover 高亮、空态、加载骨架 |

---

## 5. 关键页面改版方向

### 5.1 终端对话页 `SupportPage`（重点，展示品）

- **顶部设备条**：设备型号 chip + 地区 + 语言 + 会话号（Mono），玻璃感顶栏。
- **诊断进度轨 `DiagnosisRail`**：左侧（移动端顶部）纵向步骤条，把"状态机"可视化——已完成节点打勾、当前节点脉冲、风险节点红标、未知分支灰显。
- **消息类型视觉分化**：
  - AI 回答：teal 浅底卡片 + sparkle 标记 + 引用来源 chips（可点）。
  - 人工转接：orange 卡片 + 头像。
  - 安全停止 `SAFETY_STOP`：red 盾牌卡 + `safety` 色。
  - 诊断步骤 `StepCard`：重做为带步骤号、风险徽标、YES/NO/UNKNOWN 用 `Segmented` 选择；流式回答带 `typing` 光标与 `pulse-ring`。
- **底部输入区**：快捷操作（错误码、图片、转人工）、自适应 textarea、发送按钮 `ai` 变体。
- 氛围：聊天区背后低饱和渐变网格 + 点阵。

### 5.2 登录页 `Login.tsx`

- 左右分栏：左侧品牌 Hero（渐变 + 产品标语 + 极简"扫码→诊断→转人工"三步图）；右侧表单卡（邮箱/密码 + 语言切换）。多语言友好。

### 5.3 后台壳 `ConsoleLayout.tsx`

- 换成 `Sidebar` + `TopBar`：侧栏品牌块、分组导航（产品/二维码/文档/检索/流程/转接 + 模型/分析）、激活竖条、`backdrop-blur`；顶栏面包屑 + 租户 + 健康点 + 语言/登出。

### 5.4 各管理页

统一 `PageHeader` + `StatCard`（列表页顶部概览）+ `DataTable`/卡片网格 + `Drawer` 详情：
- **Products**：型号表 + 变体/固件用 Drawer 而非嵌套 Modal。
- **Qrs**：二维码卡片网格（含状态 Tag、有效期、吊销）。
- **Documents**：上传态/处理进度（shimmer）、生命周期 `StatusFlow`（沿用并美化）。
- **Search**：检索探针 + 命中高亮 + 适用性过滤可视化。
- **Flows**：**核心可视化**——把诊断流程渲染为节点树/有向图（节点=步骤，连线=分支 YES/NO/UNKNOWN，红色=安全停止），体现"诊断状态机"隐喻。
- **Handoffs**：由卡片列表升级为**分诊看板**（NEW / IN_PROGRESS / CLOSED 三列），卡片含风险/地区/处理人，点开 Drawer 处理。
- **Models**：模型配置表单卡。
- **Analytics**：`StatCard` 网格（带趋势）+ 事件分布（CSS 条形/分段）+ 延迟指标，呈现"仪表盘"感。

---

## 6. 实施路径（Roadmap）

| 里程碑 | 内容 | 产出 |
| --- | --- | --- |
| **M0 地基** | `tailwind.config.js` 令牌、字体自托管（`@fontsource`）、`index.css` 全局（点阵底、滚动条、动画 keyframes） | 设计变量就绪 |
| **M1 组件库 v2** | 重构 + 新增组件（§4）；`ui.tsx` 及新文件 | 可复用 UI 原子 |
| **M2 后台壳** | `ConsoleLayout` 改用 `Sidebar`+`TopBar`；`Root` 路由润色 | 统一导航 |
| **M3 终端对话页** | `SupportPage` 全改（诊断轨 + 消息分化 + 流式 + 氛围） | 展示品 |
| **M4 管理页刷新** | Products/Qrs/Documents/Search/Handoffs/Models 套用新壳与组件 | 一致性 |
| **M5 可视化页** | Flows 节点图、Analytics 仪表盘 | 差异化亮点 |
| **M6 收口** | 动效、空状态插画、移动端响应式、a11y（对比度/焦点/`aria`）、性能 | 上线质量 |

---

## 7. 验收标准（Definition of Done）

- 字体不再是 Inter；teal/orange/red 三元视觉语言在全站一致。
- 两类用户面（终端/后台）共用一套设计令牌与组件。
- `SupportPage` 能直观看出"当前诊断到第几步、是否有风险、是 AI 还是人工"。
- 所有交互有基本反馈（hover/loading/成功）；关键页面有入场动效但不喧宾夺主。
- 全部文案经 `t()`，新增 UI 文案纳入 i18n 扫描（zh/en 完整，de/fr/es 回退 en）。
- 对比度满足 WCAG AA；键盘可达；移动端（终端页）可用。

---

## 8. 风险与约束

- **i18n**：禁止硬编码文案；新增字符串须进 `locales/{zh,en}.ts` 并跑 `check:i18n`。
- **依赖**：动效用纯 CSS，不引入 framer-motion；字体用 `@fontsource` 自托管（不上外链）。
- **兼容性**：组件签名尽量向后兼容；若改签名须同步更新所有调用点（已用 grep 核对 `Button/Tag/Card/Input/Textarea/Select/Modal/StatusFlow` 的调用）。
- **性能**：点阵底用 CSS 渐变，不引图片；动画用 `transform/opacity`，避免重排。
