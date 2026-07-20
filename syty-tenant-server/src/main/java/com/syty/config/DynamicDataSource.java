package com.syty.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态数据源 - 基于 PostgreSQL Schema 隔离
 * 
 * 原理：
 * 1. 根据 TenantContextHolder 中的 tenantCode 决定目标 Schema。
 * 2. 获取连接时，动态执行 SET search_path TO tenant_xxx, public。
 * 3. 实现租户间的数据逻辑隔离，物理共享。
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataSource.class);
    
    // 默认数据源 (用于查询 public 表的索引、租户元数据等)
    private DataSource defaultTargetDataSource;

    @Override
    protected Object determineCurrentLookupKey() {
        // 返回租户编码，用于路由
        return TenantContextHolder.getTenantCode();
    }

    @Override
    public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
        this.defaultTargetDataSource = (DataSource) defaultTargetDataSource;
        super.setDefaultTargetDataSource(defaultTargetDataSource);
    }

    /**
     * 核心方法：获取连接时动态切换 Schema
     */
    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        applySearchPath(conn);
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = super.getConnection(username, password);
        applySearchPath(conn);
        return conn;
    }

    /**
     * 执行 SET search_path
     */
    private void applySearchPath(Connection conn) {
        String tenantCode = TenantContextHolder.getTenantCode();
        
        // 如果没有租户上下文，使用默认数据源 (public)
        if (tenantCode == null || tenantCode.isBlank()) {
            return; 
        }

        // 安全校验：只允许字母、数字、下划线，防止 SQL 注入
        if (!tenantCode.matches("^[a-zA-Z0-9_]+$")) {
            log.error("Invalid tenantCode format: {}", tenantCode);
            throw new RuntimeException("Invalid tenantCode format");
        }

        String schemaName = "tenant_" + tenantCode;
        String sql = "SET search_path TO " + schemaName + ", public";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.debug("Set search_path to: {}", sql);
        } catch (SQLException e) {
            log.error("Failed to set search_path to {}: {}", schemaName, e.getMessage());
            // 注意：这里通常不抛异常，让 SQL 执行去报错 (relation not found)，
            // 或者在此处捕获并包装为更友好的错误。
        }
    }

    /**
     * 构建动态数据源
     * 
     * @param defaultDataSource 默认数据源 (连接 public)
     * @param targetDataSources 目标数据源 Map (Key: tenantCode, Value: DataSource)
     *                          注意：在 Schema 隔离模式下，Value 通常也是同一个 DataSource 实例，
     *                          只是通过 search_path 区分。
     */
    public static DynamicDataSource create(DataSource defaultDataSource) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        
        // 设置默认数据源
        dynamicDataSource.setDefaultTargetDataSource(defaultDataSource);
        
        // 在 Schema 隔离模式下，所有租户共用同一个物理连接池
        // 我们通过覆写 getConnection 来动态切换 schema
        // 因此 targetDataSources 可以为空，或者指向同一个 defaultDataSource
        Map<Object, Object> targetMap = new HashMap<>();
        // targetMap.put("default", defaultDataSource); 
        
        dynamicDataSource.setTargetDataSources(targetMap);
        dynamicDataSource.afterPropertiesSet();
        
        return dynamicDataSource;
    }
}
