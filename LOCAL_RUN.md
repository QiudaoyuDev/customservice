# 本地启动说明（开发机）

本文件记录了在本机（Windows）把前后端跑起来所需的命令与凭据。
仅供本地开发使用，生产环境请改用 `infra/compose.yaml` 与正式密钥。

管理员账号和密码必须通过 `BOOTSTRAP_ADMIN_EMAIL`、`BOOTSTRAP_ADMIN_PASSWORD` 在本机环境中设置；不在仓库、页面默认值或启动脚本中提供演示凭据。登录地址为 http://localhost:5173，必须手动输入凭据。

## 环境要求

- JDK 21：`C:\Program Files\Java\jdk-21.0.11`
- Maven：仓库自带 `repos/backend/mvnw.cmd`，会下载固定的 Maven 3.9.11；无需依赖系统 Maven。
- Node.js 24：`C:\Program Files\nodejs`
- PostgreSQL 16（Windows 服务 `postgresql-x64-16`，已建库 `support`，密码 `postgres`）
- 外部依赖（MinIO / OCR / Embedding / Qdrant）当前为占位地址，本机未启动，
  因此 `/actuator/health` 返回 503，涉及存储/检索的功能暂不可用。

## 1. 前端（Vite，端口 5173）

```powershell
cd c:\myproject\customservice\repos\frontend

# 严格按锁文件安装依赖
npm ci

# 启动开发服务器
npm run dev
# → http://localhost:5173
```

`vite.config.ts` 已将 `/api` 与 `/public` 代理到后端 `http://localhost:8080`。

## 2. 后端（Spring Boot，端口 8080）

需先确保 PostgreSQL 服务已启动：

```powershell
# 若服务未运行
Start-Service postgresql-x64-16
```

构建并启动：

```powershell
cd c:\myproject\customservice\repos\backend

# 必须用 JDK 21
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'

# 清理、测试、打包
.\mvnw.cmd clean test package

# 配置环境变量并启动
$env:SPRING_PROFILES_ACTIVE        = 'dev'
$env:DATABASE_URL                  = 'jdbc:postgresql://localhost:5432/hardware_ai_support'
$env:DATABASE_USERNAME              = '<local database user>'
$env:DATABASE_PASSWORD              = '<local database password>'
$env:JWT_SECRET                     = '<at least 32 characters>'
$env:QR_TOKEN_SECRET                = '<at least 32 characters>'
$env:BOOTSTRAP_ADMIN_EMAIL          = '<administrator email>'
$env:BOOTSTRAP_ADMIN_PASSWORD       = '<administrator password>'
$env:MINIO_ENDPOINT                 = 'http://localhost:9000'
$env:MINIO_ACCESS_KEY               = '<MinIO application user>'
$env:MINIO_SECRET_KEY               = '<MinIO application secret>'
$env:MINIO_BUCKET                   = 'support-assets'
$env:OCR_URL                        = 'http://localhost:18081'
$env:EMBEDDING_URL                  = 'http://localhost:18082'
$env:RERANK_URL                     = 'http://localhost:18082'
$env:QDRANT_URL                     = 'http://localhost:6333'
$env:QDRANT_API_KEY                 = '<Qdrant API key>'

java -jar target/support-api-0.1.0-SNAPSHOT.jar
# → http://localhost:8080
```

## 3. 验证

- 前端页面：http://localhost:5173
- 后端登录接口（免认证）：`POST http://localhost:8080/api/auth/login`；使用本机环境中设置的管理员凭据。
- 带 JWT 调 `GET http://localhost:8080/api/products` 可获取数据（当前为空）

## 备注

- `mvnw.cmd` 的下载缓存位于 `repos/backend/.mvn/wrapper/dists/`，已被 Git 忽略。
- 完整生产基础设施（PostgreSQL/MinIO/Qdrant/OCR/Embedding）请参考 `infra/DEPLOYMENT.md`，
  安装 Docker Desktop 后用 `infra/compose.yaml` 启动。
