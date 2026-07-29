# 长期记忆 (MEMORY)

## 项目：出海硬件厂商多语言 AI 售后诊断与客服平台
- 仓库：`repos/backend`（Spring Boot 3.3.2 / Java 21）、`repos/frontend`（React 19 + Vite + TS + Tailwind + react-i18next）、`infra`（Docker Compose）。
- 前端技术栈与约定见 `repos/frontend/README`/根 `README.md`。i18n 资源内联于 `src/i18n/locales/{en,zh}.ts`，`LANGS`/`langNames`/`statusLabel`/`regionLabel` 由 `src/i18n/index.tsx` 导出。
- 语言检测顺序：localStorage(`app.lang`) → navigator → htmlTag；后台手动切换器在 `ConsoleLayout` 底部；终端页切换器在 `SupportPage` 头部。
- 设计系统令牌（brand/ai/human/ink/line/ok/warn/danger/info）定义在 `repos/frontend/tailwind.config.js`，与 `docs/UI方案.md` 一致。`src/styles.css` 是早期原型死代码，已不被引入。
- 后端 i18n：`messages_zh.properties` / `messages_en.properties`（`repos/backend/.../i18n`）。
