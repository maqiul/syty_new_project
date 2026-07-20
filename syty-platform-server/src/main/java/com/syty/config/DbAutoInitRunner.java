package com.syty.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 数据库自动初始化脚本
 * V1.9 临时使用：确保 package_info 表存在并插入默认数据
 */
@Component
public class DbAutoInitRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DbAutoInitRunner.class);
    private final DataSource dataSource;

    public DbAutoInitRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        log.info(">>> 开始执行数据库自动初始化 (DbAutoInitRunner)...");
        
        String createTableSql = 
            "CREATE TABLE IF NOT EXISTS public.package_info (" +
            "    id BIGSERIAL PRIMARY KEY, " +
            "    name VARCHAR(100) NOT NULL, " +
            "    price DECIMAL(10, 2) DEFAULT 0.00, " +
            "    max_tenants INT DEFAULT 0, " +
            "    duration_days INT DEFAULT 365, " +
            "    features TEXT, " +
            "    status INT DEFAULT 1, " +
            "    deleted INT DEFAULT 0, " +
            "    created_at TIMESTAMP DEFAULT NOW(), " +
            "    updated_at TIMESTAMP DEFAULT NOW()" +
            ")";

        String insertSeedSql = 
            "INSERT INTO public.package_info (id, name, price, duration_days, status) " +
            "VALUES (1, '基础版', 99.00, 365, 1) " +
            "ON CONFLICT (id) DO NOTHING";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 1. 建表
            stmt.execute(createTableSql);
            log.info("✅ 表 package_info 检查完毕 (已存在或已创建)");

            // 2. 插入种子数据
            stmt.execute(insertSeedSql);
            log.info("✅ 种子数据插入完毕");

        } catch (Exception e) {
            log.error("❌ 数据库自动初始化失败: {}", e.getMessage());
        }
    }
}
