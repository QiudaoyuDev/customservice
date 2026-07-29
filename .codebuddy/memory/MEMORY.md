# 长期记忆 (MEMORY)

## 项目：出海硬件厂商多语言 AI 售后诊断与客服平台
- 仓库：`repos/backend`（Spring Boot 3.3.2 / Java 21）、`repos/frontend`（React 19 + Vite + TS + Tailwind + react-i18next）、`infra`（Docker Compose）。
- 前端技术栈与约定见 `repos/frontend/README`/根 `README.md`。i18n 资源内联于 `src/i18n/locales/{en,zh}.ts`，`LANGS`/`langNames`/`statusLabel`/`regionLabel` 由 `src/i18n/index.tsx` 导出。
- 语言检测顺序：localStorage(`app.lang`) → navigator → htmlTag；后台手动切换器在 `ConsoleLayout` 底部；终端页切换器在 `SupportPage` 头部。
- 设计系统令牌（brand/ai/human/ink/line/ok/warn/danger/info）定义在 `repos/frontend/tailwind.config.js`，与 `docs/UI方案.md` 一致。`src/styles.css` 是早期原型死代码，已不被引入。
- 后端 i18n：`messages_zh.properties` / `messages_en.properties`（`repos/backend/.../i18n`）。
- **UI 专项设计方向（Precision Instrument Console / 精密仪表控制台）**：见 `docs/UI方案.md`（原 README 引用但缺失，已于 2026-07-29 创建）。核心：沉稳信任感、诊断可视化、AI(teal)↔Human(orange)↔Safety(red) 三元视觉语言。字体弃用 Inter，改为 Sora(Display)+Manrope(Body)+JetBrains Mono(数据)，经 `@fontsource` 自托管。动效纯 CSS，不引 framer-motion。色彩令牌在 `repos/frontend/tailwind.config.js`。
- UI 关键词约束：避免紫渐变 AI 风、消费级可爱风、Inter/Space Grotesk 字体；强调层级、氛围（蓝图点阵底/渐变网格）、状态机/分支/安全停止的可视化。
