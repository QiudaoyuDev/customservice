# Hardware Support Product System

用于终端设备诊断、人工接管和知识运营界面的产品设计系统。它保留项目现有的深青、人工橙和安全红语义，但把视觉表达从通用 SaaS 卡片系统收紧为“服务手册与工程值班台”。

## 来源

- `repos/frontend/tailwind.config.js`：现有颜色、字体、圆角、阴影和动效令牌。
- `repos/frontend/src/index.css`：全局背景、焦点态和加载动效。
- `repos/frontend/src/components/ui.tsx`：真实按钮、标签、横幅、表格、抽屉和诊断轨组件。
- `repos/frontend/src/pages/SupportPage.tsx`：真实用户诊断状态和文案结构。
- `repos/frontend/src/console/ConsoleLayout.tsx`：真实后台导航和租户结构。
- `docs/UI方案.md`：Precision Instrument Console 定位与 AI/人工/安全三元语义。
- `prototype/pilot-validation/`：上一版闭环原型及用户对模板化视觉的反馈。

## 文件

- `tokens/colors_and_type.css`：颜色、字体、尺寸及语义令牌。
- `brand/voice-and-tone.md`：面向用户、客服和运营人员的文案规则。
- `brand/style-notes.md`：布局、线条、圆角、动效和反模板规则。
- `SKILL.md`：供 OpenDesign 自动发现和复用的设计系统入口。

## 核心判断

这不是“AI 产品”视觉。用户面对的是故障和潜在安全风险，运营人员面对的是版本、证据和责任。因此界面应像一份被认真维护的设备档案：结构清晰、编号可信、风险醒目，但不过度渲染技术感。

## 确认状态

颜色语义、设备字段、诊断状态与安全层级来自现有实现，可信度高。编辑化字体、暖纸张底和减少卡片的方向来自本次用户反馈，应在原型评审后再决定是否迁移到生产 UI。
