# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指导。

## 项目概述

基于 Vue 3 + Spring Boot + 阿里云 OSS + WebSocket 的 **企业级智能协同云图库平台**。支持公开图片搜索与上传、个人私有空间、团队空间实时协同编辑、AI 图片扩图等功能。

## 仓库结构

```
yu-picture-backend/       # Spring Boot 后端（controller → service → mapper）
yu-picture-frontend/      # Vue 3 + Vite + TypeScript + Ant Design Vue 单页应用
```

前端 API 层（`src/api/`）通过 `@umijs/openapi` 从后端的 Knife4j/OpenAPI2 文档自动生成。

## 构建与开发命令

### 后端（`yu-picture-backend/`）

```bash
mvn spring-boot:run          # 启动开发服务器（端口 8123，上下文路径 /api）
mvn clean package            # 构建 JAR 包
mvn test                     # 运行测试
```

环境要求：JDK 1.8、MySQL（`yu_picture` 数据库）、Redis（127.0.0.1:6379）。SQL 初始化脚本位于 `sql/create_table.sql`。

### 前端（`yu-picture-frontend/`）

```bash
npm install                  # 安装依赖
npm run dev                  # 开发服务器，支持热重载（直接运行 vite）
npm run build                # 类型检查 + 生产构建
npm run lint                 # ESLint 自动修复
npm run format               # Prettier 格式化 src/
npm run openapi              # 根据后端 OpenAPI 规范重新生成 API 客户端代码
```

前端 API 客户端代码（`src/api/*Controller.ts`）由 `@umijs/openapi` **自动生成**。后端 API 变更后需运行 `npm run openapi`，`openapi.config.js` 中配置了后端 OpenAPI JSON 端点地址。

## 架构

### 后端（`yu-picture-backend`）

标准 Spring Boot 分层架构：
- **`controller/`** — REST 控制器（Picture、Space、SpaceUser、User、File 等）
- **`service/`** + **`service/impl/`** — 业务逻辑服务，继承 MyBatis-Plus 的 `IService`
- **`mapper/`** — MyBatis-Plus 映射器
- **`model/entity/`** — 实体类
- **`model/dto/`** — 请求 DTO（每个接口对应一个类）
- **`model/vo/`** — 响应视图对象
- **`model/enums/`** — 枚举类
- **`manager/`** — 协调模块：权限认证（`SaTokenAuthManager`、`SpaceUserAuthManager`）、文件上传（模板方法模式）、WebSocket + Disruptor、分片算法
- **`constant/`** — 静态常量
- **`config/`** — Spring 配置（跨域、MyBatis Plus 等）
- **`annotation/`** + **`aop/`** — `@AuthCheck` 注解 + AOP 拦截器，实现基于角色的访问控制
- **`api/`** — 外部 API 集成：阿里云 AI（图片扩图）、图片搜索（通过 Jsoup 抓取百度图片）
- **`exception/`** — `BusinessException`、`ErrorCode` 枚举、全局异常处理器
- **`utils/`** — 颜色相似度和颜色转换工具

### 前端（`yu-picture-frontend`）

- **`src/pages/`** — 页面组件（Vue 单文件组件），每个页面对应一个路由
- **`src/layouts/BasicLayout.vue`** — 主布局（侧边栏 + 顶栏）
- **`src/router/index.ts`** — Vue Router 路由定义
- **`src/api/`** — 自动生成的 API 客户端函数（TypeScript），与后端各控制器一一对应
- **`src/stores/useLoginUserStore.ts`** — Pinia 状态管理，存储登录用户信息
- **`src/access.ts`** — 导航守卫：`router.beforeEach` 校验管理员权限，首次加载时获取登录用户
- **`src/components/`** — 可复用组件：PictureList、PictureUpload、ImageCropper、ImageOutPainting、BatchEditPictureModal、ShareModal 等
- **`src/utils/pictureEditWebSocket.ts`** — WebSocket 客户端，用于实时协同编辑图片

### 核心技术模式

1. **权限认证**：Sa-Token 框架 + `@AuthCheck` AOP 注解（校验 `mustRole = "admin"`）+ `@SaSpaceCheckPermission` 注解实现空间级 RBAC（`viewer/editor/admin`）。空间权限配置定义在 `spaceUserAuthConfig.json` 中。

2. **分库分表**：ShardingSphere 5.2 + 自定义 `PictureShardingAlgorithm`（CLASS_BASED），按 `spaceId` 对 `picture` 表进行分片。公共空间图片（spaceId=null）路由到默认表。

3. **实时协同**：WebSocket（`ws://localhost:8123/api/ws/picture/edit`）配合 **Disruptor 环形缓冲区**（LMAX）实现高吞吐消息处理。编辑变更广播给同一图片的所有在线编辑者。

4. **缓存**：Redis + Caffeine 二级缓存。Caffeine 用于热点数据本地缓存；Redis 用于分布式 Session 存储。

5. **文件上传**：模板方法模式（`PictureUploadTemplate`），两种实现 —— 通过 OSS 上传文件（`FilePictureUpload`）和通过 URL 抓取上传（`UrlPictureUpload`）。

6. **图片搜索**：通过 Jsoup 抓取百度图片 —— 多步骤门面模式（`ImageSearchApiFacade`）：获取页面 URL → 图片列表 URL → 首张图片 URL。

## 注意事项

- 前端开发服务器默认**不会**将 `/api` 代理到后端（`vite.config.ts` 中的代理配置已被注释）。需要手动配置代理或确保后端与前端部署在同一主机。
- 项目排除了 `ShardingSphereAutoConfiguration`——ShardingSphere 改为通过 application.yml 手动配置。
- `resources` 下的 `biz/` 目录包含运行时配置文件：`spaceUserAuthConfig.json`（空间权限矩阵）和 `vipCode.json`（VIP 兑换码）。
