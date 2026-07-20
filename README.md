# 三益穿线系统 (syty) — 多租户版

球场穿线业务管理系统，前后端分离架构，完整多租户支持。

## 多租户架构

```
平台方 (SUPER_ADMIN)
│
├── 租户管理（创建/禁用/删除租户）
├── 公共数据维护（球员/球拍/球线）
│
├── 租户A (TENANT_ADMIN + STAFF)    租户B (TENANT_ADMIN + STAFF)
│   ├── 店铺管理                      ├── 店铺管理
│   ├── 球线库存与定价                ├── 球线库存与定价
│   └── 穿线订单                      └── 穿线订单
│
│   player/racket/string_info ← 共享公共数据
```

### 数据隔离规则

| 表 | 归属 | 说明 |
|----|------|------|
| `tenant` | **平台** | 租户列表，仅SUPER_ADMIN可见 |
| `sys_user` | **租户私有** | 登录自动过滤 tenant_id |
| `player` | **平台公共** | 所有租户共享引用 |
| `racket` | **平台公共** | 所有租户共享引用 |
| `string_info` | **平台公共** | 所有租户共享引用 |
| `shop` | **租户私有** | 自动加 tenant_id 过滤 |
| `shop_string` | **租户私有** | 自动加 tenant_id 过滤 |
| `stringing_order` | **租户私有** | 自动加 tenant_id 过滤 |

### 权限分级

| 角色 | 权限 |
|------|------|
| **SUPER_ADMIN** | 管理所有租户 + 维护平台公共数据（球员/球拍/球线） |
| **TENANT_ADMIN** | 管理本租户的店铺、球线库存、订单、店员账号 |
| **STAFF** | 操作绑定店铺的订单业务 |

## 项目结构

```
I:\syty
├── database/init.sql              # 建库建表（多租户完整表结构）
├── syty-server/                   # Spring Boot 3 后端
├── syty-web/                      # Vue 3 + Element Plus 前端
└── syty-print-client/             # C# 本地打印客户端
```

## 技术栈

- **后端:** Spring Boot 3.2 + MyBatis-Plus + Spring Security + JWT + Netty
- **前端:** Vue 3 + Vite + Element Plus + Pinia
- **数据库:** MySQL 8.0+
- **打印通信:** Netty TCP (Java → C#)
- **多租户:** MyBatis-Plus TenantLineInnerInterceptor（自动 SQL 过滤）

## 快速启动

```bash
# 1. 建库
执行 database/init.sql

# 2. 后端
cd syty-server && mvn spring-boot:run    # 端口 8080

# 3. 前端
cd syty-web && npm install && npm run dev # 端口 3000

# 4. 打印客户端
cd syty-print-client && dotnet run        # 端口 18889
```

## 默认账号

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| `admin` | `admin123` | SUPER_ADMIN | 平台超级管理员 |

> 租户管理员账号需要在系统中创建租户后，由 SUPER_ADMIN 在后台添加。
