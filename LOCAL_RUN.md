# 本地启动说明（开发机）

本文件记录了在本机（Windows）把前后端跑起来所需的命令与凭据。
仅供本地开发使用，生产环境请改用 `infra/compose.yaml` 与正式密钥。

## 管理员初始凭据

| 项 | 值 |
| --- | --- |
| 邮箱 | `admin@hardwareai.com` |
| 密码 | `Admin@123456` |

> 由后端 `BOOTSTRAP_ADMIN_PASSWORD` 注入，仅在数据库为空时创建一次。
> 登录地址：http://localhost:5173 （前端登录页已预填邮箱，密码留空需手动输入）。

## 环境要求

- JDK 21：`C:\Program Files\Java\jdk-21.0.11`
- Maven 3.9：`C:\dev-tools\apache-maven-3.9.16`
- Node.js 24：`C:\Program Files\nodejs`
- PostgreSQL 16（Windows 服务 `postgresql-x64-16`，已建库 `support`，密码 `postgres`）
- 外部依赖（MinIO / OCR / Embedding / Qdrant）当前为占位地址，本机未启动，
  因此 `/actuator/health` 返回 503，涉及存储/检索的功能暂不可用。

## 1. 前端（Vite，端口 5173）

```powershell
cd c:\myproject\customservice\repos\frontend

# 安装依赖（package-lock 原指向已下线的私有仓库，故显式指定公共 registry）
npm install --registry https://registry.npmjs.org/

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

# 必须用 JDK 21（Maven 默认可能指向 JDK 8）
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'

# 打包（跳过测试）
& 'C:\dev-tools\apache-maven-3.9.16\bin\mvn.cmd' -DskipTests package

# 配置环境变量并启动
$env:SPRING_PROFILES_ACTIVE        = 'dev'
$env:SPRING_DATASOURCE_URL         = 'jdbc:postgresql://localhost:5432/support'
$env:SPRING_DATASOURCE_USERNAME    = 'postgres'
$env:SPRING_DATASOURCE_PASSWORD    = 'postgres'
$env:JWT_SECRET                    = 'dev-secret-key-please-change-in-production-1234567890'
$env:BOOTSTRAP_ADMIN_EMAIL         = 'admin@hardwareai.com'
$env:BOOTSTRAP_ADMIN_PASSWORD      = 'Admin@123456'
$env:MINIO_ENDPOINT                = 'http://localhost:9000'
$env:MINIO_ACCESS_KEY              = 'minioadmin'
$env:MINIO_SECRET_KEY              = 'minioadmin'
$env:MINIO_BUCKET                  = 'support'
$env:OCR_SERVICE_URL               = 'http://localhost:8083'
$env:EMBEDDING_SERVICE_URL         = 'http://localhost:8084'
$env:QDRANT_HOST                   = 'localhost'
$env:QDRANT_PORT                   = '6333'
$env:QDRANT_API_KEY                = ''

java -jar target/support-api-0.1.0-SNAPSHOT.jar
# → http://localhost:8080
```

## 3. 验证

- 前端页面：http://localhost:5173
- 后端登录接口（免认证）：`POST http://localhost:8080/api/auth/login`
  请求体 `{"email":"admin@hardwareai.com","password":"Admin@123456"}` 返回 JWT
- 带 JWT 调 `GET http://localhost:8080/api/products` 可获取数据（当前为空）

## 备注

- 原 `frontend/package.json` 缺失 `@vitejs/plugin-react` 与 `vite.config.ts`，已补全。
- 后端 `mvnw.cmd` 期望 Maven 在 `D:\dev-tools\apache-maven-3.9.16-bin\...`，
  实际安装在 `C:\dev-tools\apache-maven-3.9.16`，故直接用该路径的 `mvn.cmd`。
- 完整生产基础设施（PostgreSQL/MinIO/Qdrant/OCR/Embedding）请参考 `infra/DEPLOYMENT.md`，
  安装 Docker Desktop 后用 `infra/compose.yaml` 启动。
