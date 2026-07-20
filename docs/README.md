# 舜羽穿线 SaaS 平台 - 项目交付文档

> **版本**: V2.2
> **更新日期**: 2026-05-18
> **状态**: ✅ 已交付

---

## 📚 1. 文档导航

| 文档名称 | 说明 | 链接 | 负责人 |
|:---|:---|:---|:---|
| **《项目总览与变更记录》** | 项目全生命周期变更记录 | [CHANGELOG.md](../CHANGELOG.md) | 嘎嘎 |
| **《用户操作手册》** | 租户端操作指南，适合门店老板 | [User_Manual.md](User_Manual.md) | 老赵 |
| **《系统架构设计文档》** | 架构图、技术栈、多租户隔离方案 | [Architecture.md](Architecture.md) | 老K |
| **《API 接口文档》** | 核心后端接口清单、参数说明 | [API_Doc.md](API_Doc.md) | 老刘 |
| **《环境部署与运维指南》** | 数据库、Redis、前后端部署步骤 | [Deployment_Guide.md](Deployment_Guide.md) | 老孙 |

---

## 🚀 2. 项目快速启动 (Quick Start)

### 2.1 核心依赖
- **JDK**: 17+
- **Node.js**: 18+
- **PostgreSQL**: 15+
- **Redis**: 7.0+

### 2.2 数据库初始化
```bash
# 1. 启动 PostgreSQL
podman run -d -p 5432:5432 --name syty-db -e POSTGRES_PASSWORD=your_password postgres:15

# 2. 创建数据库
podman exec -it syty-db psql -U postgres -c "CREATE DATABASE syty;"

# 3. 执行 V1->V2.2 脚本 (详见 Deployment_Guide.md)
```

### 2.3 后端启动
```bash
cd syty-platform-server
mvn clean package -DskipTests
java -jar target/syty-platform-server.jar
```

### 2.4 前端启动
```bash
cd syty-tenant
npm install
npm run dev
```

---

## 🏆 3. V2.2 交付功能清单

### ✅ 核心业务 (Core Features)
- [x] **多租户隔离**: 动态数据源 + Schema 隔离 (V1.9)
- [x] **H5 扫码下单**: 客户自主下单、选线、选磅数
- [x] **门店接单流转**: 接单、穿线中、完工、交付
- [x] **库存管理**: 线材入库、扣减、预警
- [x] **会员储值**: 余额支付、充值记录、退款回退

### ✅ 营销与运营 (Marketing & Ops)
- [x] **优惠券系统**: 领券、发券、核销、防超发 (Redis 锁)
- [x] **穿线师排班**: 排班日历、冲突检测、批量提交
- [x] **老板数据看板**: 营收趋势、会员增长、缓存加速
- [x] **强制调账**: 异常订单资金修正

### ✅ 体验与基建 (UX & Infra)
- [x] **导入导出**: 会员/库存 Excel 导入导出
- [x] **数据脱敏**: 手机号隐藏、敏感日志过滤
- [x] **错误处理**: 全局异常拦截，拒绝暴露堆栈
- [x] **缓存机制**: 看板数据 Redis 缓存 + 自动失效
- [x] **幂等防重**: 防止重复点击导致的重复扣款

---

## 👥 4. 团队致谢

- **老马**: 项目发起人、产品把控
- **嘎嘎**: 项目管理、进度协调
- **老赵**: 产品策划、操作手册
- **老K**: 架构设计、技术选型
- **老刘**: 后端开发、API 实现
- **小王**: 前端开发、UI 交互
- **老孙**: 运维部署、环境搭建
- **老李**: 数据库管理、SQL 优化
- **小查**: 质量保证、测试验收
