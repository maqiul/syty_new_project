# 舜羽穿线 SaaS 平台 (Syty SaaS)

## 📁 项目架构总览
本项目采用 **双前端 + 双后端** 的完全分离架构，实现平台运营端与租户业务端的物理级隔离。
数据库采用 PostgreSQL **Schema 级多租户隔离** (V1.9 架构)。

### 📦 核心项目清单

| 项目名称 | 路径 | 角色 | 端口 | 技术栈 | 核心职责 |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **平台端前端** | `syty-web` | 平台超管后台 | 3000 | Vue3/TS | 租户管理、套餐定义、运营公告、系统配置 |
| **平台端后端** | `syty-platform-server` | 平台服务 | 8080 | Spring Boot 3 | 管理 `public` 模式数据，处理租户生命周期、商业化计费 |
| **租户端前端** | `syty-tenant` | 门店租户后台 | 3001 | Vue3/TS | 穿线工单、客户管理、库存、接收平台公告 |
| **租户端后端** | `syty-tenant-server` | 租户服务 | 8081 | Spring Boot 3 | 动态切换 `tenant_xxx` Schema，处理租户内部业务逻辑 |

---

## 🚀 核心功能模块状态 (Status)

### 1. ✅ 核心架构 (Core)
- **多租户隔离**：已实现 PostgreSQL Schema 级数据隔离。通过 `DynamicDataSource` 装饰器模式在获取连接时注入 `SET search_path`。
- **双端分离**：平台端 (`public`) 与租户端 (`tenant_xxx`) 代码、数据库、端口完全独立。
- **权限分治**：RBAC 权限控制，平台端与租户端 Token 命名强制区分 (`plat-token` vs `tenant-token`)。

### 2. ✅ 租户全生命周期 (Tenant Lifecycle)
- **自动开通**：平台新增租户时，自动创建专属 Schema 并初始化种子数据（管理员账号 `admin`/`admin123`）。
- **套餐绑定**：新建租户强制选择套餐，自动计算 `package_expired_at`。
- **到期拦截**：租户登录时校验套餐有效期，过期自动阻断登录并提示续费。

### 3. ✅ 商业化续费系统 (SaaS Billing)
- **SaaS 级续费逻辑**：
  - 未过期续费：在原到期时间上叠加。
  - 已过期续费：从当前时间开始计算（记录空档期 `gap_days`）。
- **防恶意缩短**：如果操作导致到期时间提前，强制要求填写备注原因。
- **全快照日志**：所有续费/升降级操作均记录 `tenant_package_log`，包含操作前后时间快照、操作人信息。

### 4. 🚧 V2.0 运营中心 (In Progress)
- **运营公告**：
  - 平台端 (`syty-web`)：已实现公告发布页 (`/operation/notice`)。
  - 租户端 (`syty-tenant`)：已实现首页铃铛通知与公告拉取。
- **全局配置**：
  - 平台端 (`syty-web`)：已实现 Logo/名称修改页 (`/system/config`)。
- **监控预警**：
  - 登录埋点：租户登录时异步回写 `last_login_at` 到平台库，用于识别“僵尸租户”。

---

## 📝 部署说明
- **数据库**：PostgreSQL (运行于 Podman 容器 `postgres`)
- **前端运行**：`npm run dev` (Platform: 3000, Tenant: 3001)
- **后端运行**：`java -jar` (Platform: 8080, Tenant: 8081)
- **代理规则**：
  - `syty-web` 代理 `/api/platform` -> 8080
  - `syty-tenant` 代理 `/api/tenant` -> 8081
