package com.syty.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * 租户端数据库自动初始化
 * <p>
 * 启动时：
 * 1. 查询所有已存在的 tenant_xxx schema
 * 2. 对每个 schema 执行 V2.0_supplier.sql（供应商表）
 * <p>
 * 所有 DDL 均使用 IF NOT EXISTS，保证幂等。
 */
@Component
@Order(100) // 确保在 SaToken/MyBatis 配置完成后执行
public class TenantDbAutoInitRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantDbAutoInitRunner.class);

    /** 启动时对所有 tenant schema 执行的脚本 */
    private static final String[] TENANT_SCRIPTS = {
            "sql/V2.0_supplier.sql"
    };

    private final DataSource dataSource;

    public TenantDbAutoInitRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        log.info(">>> 开始执行租户端数据库自动初始化 (TenantDbAutoInitRunner)...");

        // 1. 查询所有已存在的 tenant_xxx schema
        List<String> schemas = listTenantSchemas();
        if (schemas.isEmpty()) {
            log.info("未发现租户 Schema，跳过租户级脚本。");
            return;
        }
        log.info("发现 {} 个租户 Schema: {}", schemas.size(), schemas);

        int successCount = 0;
        int failCount = 0;

        // 2. 对每个 schema 执行租户级脚本
        for (String schema : schemas) {
            for (String scriptPath : TENANT_SCRIPTS) {
                try {
                    executeScriptInSchema(schema, scriptPath);
                    log.info("✅ [{}] 已执行: {}", schema, scriptPath);
                    successCount++;
                } catch (Exception e) {
                    log.error("❌ [{}] 执行脚本失败: {} - {}", schema, scriptPath, e.getMessage());
                    failCount++;
                }
            }
        }
        log.info("<<< 租户端数据库自动初始化完成: 成功 {} 个, 失败 {} 个 <<<", successCount, failCount);
    }

    /** 查询所有 tenant_xxx schema */
    private List<String> listTenantSchemas() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                     "SELECT schema_name FROM information_schema.schemata " +
                     "WHERE schema_name LIKE 'tenant\\_%' ESCAPE '\\' ORDER BY schema_name")) {
            List<String> result = new java.util.ArrayList<>();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result;
        } catch (Exception e) {
            log.warn("查询租户 Schema 失败: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /** 在指定 schema 下执行脚本 (通过 SET search_path 切换) */
    private void executeScriptInSchema(String schemaName, String scriptPath) throws Exception {
        Resource resource = new ClassPathResource(scriptPath);
        if (!resource.exists()) {
            log.warn("脚本不存在, 跳过: {}", scriptPath);
            return;
        }
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // 切换 search_path 到租户 schema
            stmt.execute("SET search_path TO \"" + schemaName + "\"");
            // 执行脚本
            ScriptUtils.executeSqlScript(conn, resource);
        }
    }
}
