# Docker 部署指南

## 快速开始

### 1. 准备环境变量

```bash
# 复制环境变量文件
cp .env.example .env

# 编辑 .env 文件，设置数据库密码等
vim .env
```

### 2. 启动服务

**仅租户端（开发测试）：**
```bash
docker-compose up -d
```

**完整平台（平台端 + 租户端）：**
```bash
docker-compose -f docker-compose.platform.yml up -d
```

### 3. 访问服务

| 服务 | 地址 | 说明 |
|------|------|------|
| 平台端前端 | http://localhost:3000 | 平台管理后台 |
| 租户端前端 | http://localhost:3001 | 租户业务后台 |
| 平台端后端 | http://localhost:8080 | 平台 API |
| 租户端后端 | http://localhost:8081 | 租户 API |
| PostgreSQL | localhost:5432 | 数据库 |
| Redis | localhost:6379 | 缓存 |
| MQTT | localhost:1883 | 消息队列 |

## 数据库初始化

首次启动时，数据库会自动执行 `database/` 目录下的 SQL 脚本。

如果需要手动执行：

```bash
# 进入容器
docker exec -it syty-postgres psql -U root -d syty

# 执行 SQL
\i /docker-entrypoint-initdb.d/V2.0_supplier.sql
\i /docker-entrypoint-initdb.d/V2.1_permissions.sql
```

## 常用命令

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f syty-tenant-server

# 重启服务
docker-compose restart syty-tenant-server

# 停止服务
docker-compose down

# 停止并删除数据卷（谨慎！）
docker-compose down -v

# 重新构建镜像
docker-compose build --no-cache
```

## 生产部署建议

1. **修改默认密码**
   - 数据库密码（.env 文件）
   - JWT 密钥（application.yml）

2. **配置 HTTPS**
   - 使用 Nginx 反向代理
   - 配置 SSL 证书

3. **数据备份**
   ```bash
   # 备份数据库
   docker exec syty-postgres pg_dump -U root syty > backup_$(date +%Y%m%d).sql
   
   # 恢复数据库
   docker exec -i syty-postgres psql -U root syty < backup.sql
   ```

4. **监控**
   - 配置日志收集
   - 设置健康检查告警

## 故障排查

```bash
# 查看容器状态
docker ps -a

# 查看容器日志
docker logs syty-tenant-server

# 进入容器调试
docker exec -it syty-tenant-server sh

# 检查网络连接
docker network inspect syty-network
```

## 更新部署

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose up -d --build

# 或仅更新特定服务
docker-compose up -d --build syty-tenant-server
```
