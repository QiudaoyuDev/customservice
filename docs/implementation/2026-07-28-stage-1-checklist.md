# 阶段 1 实施清单

- [x] V10 新增产品变体、固件版本、二维码变体/初始固件以及会话上下文审计字段；未修改既有迁移。
- [x] 新增 `ProductApplicationService`、`QrApplicationService`、`ConversationContextService`，使产品、二维码和上下文切换经过租户边界。
- [x] 二维码保持高熵 opaque token 与 SHA-256 存储；公开解析仅返回可读产品信息、地区和硬件 revision。
- [x] 已归档产品不可创建二维码或会话；历史会话和二维码记录仍可审计。
- [x] 用户切换产品会关闭旧 context、写入用户确认上下文并清除当前诊断流程。
- [x] 管理端支持型号、变体、硬件 revision、地区、固件版本和二维码变体选择；扫码端移除硬编码运营员登录与 UUID 手填入口。
- [x] 新增产品归档、跨租户变体拒绝和有效期判断单元测试。

验证：后端 `mvnw.cmd clean test` 13 项通过（PostgreSQL Testcontainers 因 Docker 暂不处理而跳过）；前端 lint、Vitest 和生产构建通过；`git diff --check` 通过。
