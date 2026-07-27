package com.syty.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 数据库自动初始化脚本
 * <p>
 * 启动时自动执行所有 V*.sql 脚本（建表 + 种子数据 + 权限码），
 * 使用 ON CONFLICT DO NOTHING / IF NOT EXISTS 保证幂等，
 * 无需手动执行 SQL。
 */
@Component
public class DbAutoInitRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DbAutoInitRunner.class);

    /** 需要自动执行的 SQL 脚本（classpath:sql/ 下的相对路径） */
    private static final String[] AUTO_INIT_SCRIPTS = {
            "sql/V1.4_add_batch_id.sql",
            "sql/V1.5_create_punch_card.sql",
            "sql/V1.8_customer_separation.sql",
            "sql/tenant/V1.9_tenant_template.sql",
            "sql/tenant/V2.0_supplier.sql",
            "sql/V2.1_permissions.sql"
    };

    private final DataSource dataSource;

    public DbAutoInitRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        log.info(">>> 开始执行数据库自动初始化 (DbAutoInitRunner)...");
        int successCount = 0;
        int failCount = 0;
        
        for (String scriptPath : AUTO_INIT_SCRIPTS) {
            try {
                Resource resource = new ClassPathResource(scriptPath);
                if (!resource.exists()) {
                    log.warn("⚠️ 脚本不存在, 跳过: {}", scriptPath);
                    continue;
                }
                try (Connection conn = dataSource.getConnection()) {
                    ScriptUtils.executeSqlScript(conn, resource);
                }
                log.info("✅ 已执行脚本: {}", scriptPath);
                successCount++;
            } catch (Exception e) {
                log.error("❌ 执行脚本失败: {} - {}", scriptPath, e.getMessage());
                failCount++;
            }
        }
        log.info(">>> 数据库自动初始化完成: 成功 {} 个, 失败 {} 个 <<<", successCount, failCount);
    }
}
