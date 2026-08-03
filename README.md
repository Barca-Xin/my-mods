# ModsWeb · 我的 Minecraft Mod 个人站

存放并发布自己写的 Minecraft Mod（Fabric / Forge / NeoForge）。

## 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Spring Boot 3（Java 21）+ Spring Data JPA + SQLite |
| 前端 | Vue 3 + Vite + Element Plus（深色主题） |
| 文件存储 | 本地 `uploads/` 目录，`StorageService` 抽象接口（以后切阿里云 OSS 只改一个实现类） |

## 目录结构

```
backend/    Spring Boot 后端（Gradle 8.14.2 构建）
frontend/   Vue 3 前端（Vite）
data/       运行时生成的 SQLite 数据库（backend/data/mods.db）
uploads/    运行时生成的模组文件（backend/uploads/）
```

## 首次启动

### 1. 启动后端（端口 8080）

本机无系统级 Gradle，用已解压的 Gradle 8.14.2：

```bash
cd backend
"C:/Users/LEnovo/Gradle/gradle-8.14.2/bin/gradle.bat" bootRun
```

首次运行会自动：
- 创建 `data/mods.db` 和 4 张表（`ddl-auto=update`）
- 注入种子数据（AdvancedEnchanting / ChainMiner / InventorySorter，含可下载的占位 jar）

### 2. 启动前端（端口 5173）

```bash
cd frontend
npm install      # 仅首次
npm run dev
```

浏览器打开 `http://localhost:5173`。开发期 `/api`、`/files` 由 Vite 代理到 8080。

### 3. 后台登录

- 地址：`http://localhost:5173/admin/login`
- 账号：`admin`，密码：`admin123`（默认，在 `backend/src/main/resources/application.yml` 的 `app.admin.password-hash` 中为 BCrypt 哈希）

> 改密码：用 `BCryptPasswordEncoder` 生成新哈希替换 `password-hash`。

## 功能一览

**前台**
- 首页封面卡片流：封面优先、标题/版本徽章浮底、空封面自动渐变占位
- 双下拉联动筛选：「游戏版本 × 加载器」，后端按 `mod_id + MAX(release_date)` 聚合出每个模组的最新版
- 详情页版本切换器：切版本只刷新下载区，URL 用 `?v=版本号` 记录，可直接分享
- 下载即统计：点下载先 `POST /api/downloads/{versionId}` 计数，再跳转真实文件
- 依赖图谱：详情页 SVG 径向节点图，外部依赖虚线、内部模组可点击跳转

**后台**
- 发布模组：填基本信息（slug 即 fabric.mod.json 的 modId）
- 上传 jar 自动解析：`fabric.mod.json` / `META-INF/mods.toml` 自动回填 版本号 / 游戏版本 / 名称 / 依赖
- 版本管理：上传 jar 自动算 MD5、存文件；设推荐版、删版本

## 常用 API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/mods?gameVersion&loader&category&keyword&page&size` | 列表（含每模组最新版） |
| GET | `/api/mods/{slug}` | 详情 |
| GET | `/api/mods/{slug}/versions?gameVersion=` | 版本切换器数据 |
| GET | `/api/mods/{slug}/dependencies` | 依赖图谱 |
| POST | `/api/downloads/{versionId}` | 下载计数，返回文件 URL |
| POST | `/api/admin/login` | 登录拿 JWT |
| POST | `/api/admin/parse-jar` | 上传 jar 解析元数据 |
| POST | `/api/admin/mods/{id}/versions` | multipart 发布版本 |
| GET | `/api/admin/mods` | 后台模组列表（含全部版本） |

## 以后要做的

- **切 OSS**：实现 `StorageService` 的 OSS 版（`app.storage.type=oss`），下载接口返回签名 URL
- **GitHub Release 同步**：`Mod.autoSyncSource` 字段已预留，接 Webhook 打 tag 自动建版本
- **生产部署**：`npm run build` 产物可交给 Nginx；后端打 jar 用 `gradle bootJar`

## 常见问题

- **数据库删了重来**：停掉后端，删 `backend/data/mods.db` 和 `backend/uploads/`，重启即重新种子
- **上传文件过大**：`application.yml` 里 `spring.servlet.multipart.max-file-size` 默认 512MB
