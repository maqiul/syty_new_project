# 🚀 SYTY 穿线管理系统 - 项目进度全景图

> **最后更新：** 2026-07-27
> **当前版本：** V2.2 项目改进完成
> **核心架构：** Spring Boot 3 + Vue 3/TS + PostgreSQL 多租户 + uni-app 移动端
> **代码仓库：** https://github.com/maqiul/syty_new_project

---

## 📊 版本总览

| 版本 | 状态 | 核心目标 | 完成时间 |
|------|------|----------|----------|
| V1.0 - V1.3 | ✅ 已封版 | 基础架构、RBAC、店铺隔离 | 早期 |
| V1.4 | ✅ 已交付 | 打印系统升级、MQTT 回执 | 2026-05 |
| V1.5 - V1.9 | ✅ 已交付 | 精细化运营、多租户基础 | 2026-06 |
| V2.0 | ✅ 已交付 | 双后端架构 + 羽网合并 | 2026-07 |
| V2.1 | ✅ 已交付 | 权限控制 + 供应商 + 库存台账 | 2026-07 |
| V2.2 | ✅ 已交付 | 项目改进（CI/CD/测试/AOP/移动端） | 2026-07-27 |

---

## 🔥 V2.0 双后端架构 + 羽网合并

### 架构升级
- **双后端分离**：`syty-platform-server`（平台管理）+ `syty-tenant-server`（租户业务）
- **双前端分离**：`syty-web`（平台端）+ `syty-tenant`（租户端）
- **数据库多租户**：PostgreSQL Schema 级隔离（`public` + `tenant_xxx`）

### 羽网合并
- 统一资产模型：`Player`/`Racket`/`String`/`Supplier` 同时服务羽毛球和网球
- 10 个 Controller 完成代理层改造
- 前端 API 层已对接，页面层零改动

---

## 🏢 V2.1 权限控制 + 业务补全

### 权限控制
- **28 个 Controller** 已加 `@SaCheckPermission` 注解
- 权限码初始化 SQL：`V2.1_permissions.sql`
- 设计原则：查询不加权限，写接口必须权限

### 业务功能补全
- 供应商管理（Supplier CRUD）
- 库存台账（Inventory 完整功能）
- 库存流水前端页面
- 提成规则前端页面
- 客户欠款/还款功能

---

## 🛠️ V2.2 项目改进（本次交付）

### CI/CD
- GitHub Actions 流水线：`.github/workflows/ci.yml`
- 4 个并行 Job：双后端编译 + 双前端构建
- push/PR 到 main 自动触发

### 数据库自动化
- 平台端 `DbAutoInitRunner` 扩展支持多脚本
- 租户端 `TenantDbAutoInitRunner` 遍历所有 tenant schema 执行
- SQL 脚本全部幂等化（`IF NOT EXISTS` / `ON CONFLICT DO NOTHING`）

### 单元测试
- 32 个测试用例全部通过
- `StockServiceTest`：库存扣减/防超卖（10 个）
- `StringingOrderServiceTest`：订单流程（8 个）
- `TenantInitServiceTest`：租户初始化（14 个）

### 操作日志 AOP
- `@OperationLog` 注解 + `LogAspect` 切面
- 自动拦截 Controller 写操作，异步记录
- `V2.2_operation_log.sql` 建表脚本

### 移动端扩展
- 新增"我的订单"页面（`pages/orders/index.vue`）
- 按手机号查询历史订单 + 进度时间线
- 首页添加入口导航

### 构建修复
- 修复 `syty-web` antdv-next locale 路径（`es/` → `dist/`）
- 双前端 + 双后端 + 移动端全部构建通过

---

## 📦 项目结构

```
syty_new_project/
├── syty-platform-server/    # 平台端后端 (Spring Boot 3)
├── syty-tenant-server/      # 租户端后端 (Spring Boot 3)
├── syty-web/                # 平台端前端 (Vue 3 + TS + antdv-next)
├── syty-tenant/             # 租户端前端 (Vue 3 + TS + antdv-next)
├── sy-mobile/               # 移动端 (uni-app + Vue 3)
├── syty-print-client/       # C# 打印客户端
├── docker/                  # Docker 配置
├── docs/                    # 文档
└── .github/workflows/       # CI/CD
```

---

## ✅ 构建验证状态

| 模块 | 命令 | 状态 |
|------|------|------|
| 平台端后端 | `mvn compile -DskipTests` | ✅ 通过 |
| 租户端后端 | `mvn compile -DskipTests` | ✅ 通过 |
| 平台端前端 | `npx vite build` | ✅ 通过 |
| 租户端前端 | `npx vite build` | ✅ 通过 |
| 移动端 | `npm run build:h5` | ✅ 通过 |
| 单元测试 | `mvn test` | ✅ 32/32 通过 |

---

## 🐳 Docker 部署

已配置但未实际验证：
- `docker-compose.yml`（租户端）
- `docker-compose.platform.yml`（完整平台）
- 各模块 `Dockerfile`（多阶段构建）

**待办**：执行 `docker-compose up` 验证部署流程

---

## ⚠️ 待办事项

### 需要手动执行
1. **数据库建表**：在 PostgreSQL 中执行
   - `V2.0_supplier.sql`
   - `V2.1_permissions.sql`
   - `V2.2_operation_log.sql`

2. **Docker 验证**：`docker-compose up` 跑一遍

### 可选优化
- 集成测试（SpringBootTest）
- 文档进一步更新
- 移动端扩展到微信小程序

---

## 📝 变更日志

详细变更记录见 `CHANGELOG.md`

---

> **维护者**：老马
> **仓库**：https://github.com/maqiul/syty_new_project
