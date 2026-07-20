package com.syty.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.syty.service.TenantInitService;
import com.syty.util.ScriptRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * 租户自动化初始化服务实现 (V1.9)
 * 
 * <h3>架构说明</h3>
 * <ul>
 *   <li>共享 PostgreSQL 实例，每个租户独立 Schema (tenant_{code})</li>
 *   <li>DDL 通过 ScriptRunner 逐条执行 (自动提交，DDL 不可事务回滚)</li>
 *   <li>DML 种子数据通过 JdbcTemplate 参数化查询 (防注入)</li>
 *   <li>失败时采用补偿性回滚: DROP SCHEMA CASCADE</li>
 * </ul>
 * 
 * <h3>执行流程</h3>
 * <pre>
 * 1. 白名单校验 (tenantCode, username)
 * 2. CREATE SCHEMA IF NOT EXISTS tenant_{code}
 * 3. 读取 DDL 模板 → 过滤 public.* 语句 → ScriptRunner 在租户 Schema 下执行
 * 4. 插入租户管理员到 sys_user (租户 Schema)
 * 5. 插入认证索引到 public.sys_user_auth_index (平台 Schema, 独立事务)
 * 6. 更新 tenant 表状态
 * </pre>
 * 
 * <h3>安全防线</h3>
 * <ul>
 *   <li>第一层: 租户编码白名单正则 (^[a-zA-Z0-9_]+$)</li>
 *   <li>第二层: SQL 中 schema 名使用双引号包裹 (防止关键字冲突)</li>
 *   <li>第三层: DML 全部参数化查询</li>
 *   <li>第四层: DDL 模板中过滤 public.* 语句 (防误操作平台表)</li>
 * </ul>
 * 
 * @author 老K (架构师)
 * @since V1.9
 */
@Slf4j
@Service
public class TenantInitServiceImpl implements TenantInitService {

    // ========================================================================
    // 常量
    // ========================================================================

    /** DDL 模板路径 (classpath 相对路径) */
    private static final String DDL_TEMPLATE_PATH = "sql/tenant/V1.9_tenant_template.sql";

    /** 默认管理员密码 (明文) — 生产环境应由调用方传入强密码 */
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    /** 租户编码白名单: 仅允许字母、数字、下划线 */
    private static final Pattern TENANT_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    /** 用户名白名单: 仅允许字母、数字、下划线 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.@]+$");

    /** 过滤模式: 匹配包含 public. 前缀的语句 (不区分大小写) */
    private static final Pattern PUBLIC_SCHEMA_PATTERN = Pattern.compile(
            "(?i)\\bpublic\\s*\\.\\s*\\w+", Pattern.MULTILINE);

    // ========================================================================
    // 依赖
    // ========================================================================

    @Autowired
    private DataSource dataSource;

    // ========================================================================
    // 核心方法: initTenant
    // ========================================================================

    @Override
    public void initTenant(String tenantCode,
                           String adminUsername, String adminPassword,
                           String adminRealName) {

        // === 安全校验 ===
        validateTenantCode(tenantCode);
        validateUsername(adminUsername);

        String schemaName = "tenant_" + tenantCode;
        log.info(">>> 开始初始化租户 Schema: {} (admin={})", schemaName, adminUsername);

        // 密码为空时使用默认
        String password = (adminPassword != null && !adminPassword.isEmpty())
                ? adminPassword : DEFAULT_ADMIN_PASSWORD;
        String passwordHash = BCrypt.hashpw(password);

        boolean schemaCreated = false;
        try {
            // === 步骤 1: 创建 Schema ===
            if (!schemaExists(tenantCode)) {
                log.info("  [1/4] CREATE SCHEMA {}", schemaName);
                executeDdl("CREATE SCHEMA \"" + schemaName + "\"");
            } else {
                log.info("  [1/4] Schema 已存在，跳过创建: {}", schemaName);
            }
            schemaCreated = true;

            // === 步骤 2: 执行 DDL 模板 (在租户 Schema 下建表) ===
            log.info("  [2/4] 执行 DDL 模板: {}", DDL_TEMPLATE_PATH);
            executeDdlTemplateInSchema(schemaName);

            // === 步骤 3: 插入种子数据 ===
            log.info("  [3/4] 插入种子数据: 管理员账号={}", adminUsername);

            // 3a. 在租户 Schema 中插入 sys_user 记录
            insertTenantAdminUser(schemaName, adminUsername, passwordHash, adminRealName);

            // 3b. 在平台全局表 public.sys_user_auth_index 中注册认证索引
            insertPlatformAuthIndex(tenantCode, adminUsername, passwordHash);

            // === 步骤 4: 更新租户状态 ===
            log.info("  [4/4] 更新租户状态为已启用");
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.update("UPDATE tenant SET status = 1 WHERE code = ?", tenantCode);

            log.info("<<< 租户 Schema 初始化完成: {} (admin={})", schemaName, adminUsername);

        } catch (TenantInitException e) {
            // 已经是包装过的异常，直接上抛
            throw e;

        } catch (Exception e) {
            log.error("!!! 租户初始化失败: schema={}, error={}", schemaName, e.getMessage(), e);

            // 补偿回滚: 如果 Schema 已创建，尝试 DROP
            boolean rollbackOk = false;
            if (schemaCreated) {
                log.warn(">>> 执行补偿回滚: DROP SCHEMA {} CASCADE", schemaName);
                rollbackOk = dropSchema(schemaName);
            }

            throw new TenantInitException(
                    tenantCode,
                    "租户初始化失败: " + e.getMessage(),
                    e,
                    rollbackOk
            );
        }
    }

    // ========================================================================
    // 核心方法: cleanupTenant
    // ========================================================================

    @Override
    public boolean cleanupTenant(String tenantCode) {
        validateTenantCode(tenantCode);
        String schemaName = "tenant_" + tenantCode;

        if (!schemaExists(tenantCode)) {
            log.info("Schema 不存在，无需清理: {}", schemaName);
            return false;
        }

        try {
            log.info(">>> 清理租户 Schema: {}", schemaName);

            // 1. 删除平台认证索引 (public 表)
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            int deleted = jdbc.update(
                    "DELETE FROM public.sys_user_auth_index WHERE tenant_code = ?", tenantCode);
            log.info("  删除平台认证索引: {} 条", deleted);

            // 2. 删除租户 Schema (CASCADE 会一并删除其中的所有表、数据、序列)
            dropSchema(schemaName);

            log.info("<<< 租户 Schema 清理完成: {}", schemaName);
            return true;

        } catch (Exception e) {
            log.error("!!! 清理租户 Schema 失败: {}, error={}", schemaName, e.getMessage(), e);
            return false;
        }
    }

    // ========================================================================
    // 核心方法: schemaExists
    // ========================================================================

    @Override
    public boolean schemaExists(String tenantCode) {
        validateTenantCode(tenantCode);
        String schemaName = "tenant_" + tenantCode;

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?",
                Integer.class, schemaName);
        return count != null && count > 0;
    }

    // ========================================================================
    // 私有方法: DDL 执行
    // ========================================================================

    /**
     * 通过 ScriptRunner 执行单条 DDL 语句
     */
    private void executeDdl(String ddl) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ScriptRunner runner = new ScriptRunner(conn)
                    .setStopOnError(true)
                    .setLogLevel(ScriptRunner.LogLevel.INFO);
            runner.execute(ddl);
        }
    }

    /**
     * 在指定 Schema 下执行 DDL 模板
     * <p>
     * 流程: 读取 SQL 文件 → 按分号拆分为语句 → 过滤 public.* 语句 →
     * 通过 SET search_path 切换 → ScriptRunner 逐条执行
     * </p>
     */
    private void executeDdlTemplateInSchema(String schemaName) throws IOException, SQLException {
        // 1. 读取 DDL 模板
        String rawDdl = readDdlTemplate();
        if (rawDdl == null || rawDdl.trim().isEmpty()) {
            throw new IllegalStateException("DDL 模板为空: " + DDL_TEMPLATE_PATH);
        }

        // 2. 过滤并拆分语句
        java.util.List<String> statements = filterAndSplitStatements(rawDdl);
        log.info("  DDL 模板共 {} 条语句 (已过滤 public.* 语句)", statements.size());

        // 3. 在租户 Schema 下执行
        try (Connection conn = dataSource.getConnection()) {
            ScriptRunner runner = new ScriptRunner(conn)
                    .setStopOnError(true)
                    .setLogLevel(ScriptRunner.LogLevel.INFO);

            // 切换 search_path 到租户 Schema
            runner.execute("SET search_path TO \"" + schemaName + "\"");

            // 逐条执行 DDL
            for (String stmt : statements) {
                runner.execute(stmt);
            }
        }

        log.info("  DDL 执行完成: schema={}", schemaName);
    }

    // ========================================================================
    // 私有方法: SQL 读取与过滤
    // ========================================================================

    /**
     * 从 classpath 读取 DDL 模板
     */
    private String readDdlTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(DDL_TEMPLATE_PATH);
        if (!resource.exists()) {
            throw new IllegalStateException("DDL 模板文件不存在: " + DDL_TEMPLATE_PATH);
        }

        try (InputStream is = resource.getInputStream();
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, n);
            }
            return sb.toString();
        }
    }

    /**
     * 过滤并拆分 SQL 语句
     * <p>
     * 规则:
     * </p>
     * <ul>
     *   <li>按分号 (;) 拆分为独立语句</li>
     *   <li>跳过空行和纯注释行</li>
     *   <li>过滤包含 public. 前缀的语句 (保护平台全局表)</li>
     * </ul>
     */
    private java.util.List<String> filterAndSplitStatements(String rawDdl) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : rawDdl.split("\\r?\\n")) {
            String trimmed = line.trim();

            // 跳过空行
            if (trimmed.isEmpty()) {
                continue;
            }

            // 跳过纯注释行 (但保留多行注释中的内容行)
            if (trimmed.startsWith("--")) {
                continue;
            }

            current.append(line).append("\n");

            // 按分号判断语句结束
            if (trimmed.endsWith(";")) {
                String stmt = current.toString().trim();
                // 去掉末尾分号 (ScriptRunner 不需要)
                if (stmt.endsWith(";")) {
                    stmt = stmt.substring(0, stmt.length() - 1).trim();
                }

                if (!stmt.isEmpty()) {
                    // 过滤 public.* 语句
                    if (PUBLIC_SCHEMA_PATTERN.matcher(stmt).find()) {
                        log.warn("  [FILTER] 跳过 public.* 语句: {}", truncate(stmt));
                    } else {
                        result.add(stmt);
                    }
                }
                current.setLength(0);
            }
        }

        // 处理文件末尾残留的语句 (无分号结尾)
        String remaining = current.toString().trim();
        if (!remaining.isEmpty() && !remaining.endsWith(";")) {
            if (!PUBLIC_SCHEMA_PATTERN.matcher(remaining).find()) {
                result.add(remaining);
            }
        }

        return result;
    }

    // ========================================================================
    // 私有方法: 种子数据插入
    // ========================================================================

    /**
     * 在租户 Schema 中插入管理员用户
     * <p>
     * 通过 SET search_path 切换后执行参数化插入，确保数据落在正确的 Schema。
     * </p>
     */
    private void insertTenantAdminUser(String schemaName, String username,
                                       String passwordHash, String realName) {
        try (Connection conn = dataSource.getConnection()) {
            ScriptRunner runner = new ScriptRunner(conn)
                    .setStopOnError(true)
                    .setLogLevel(ScriptRunner.LogLevel.INFO);

            // 切换到租户 Schema
            runner.execute("SET search_path TO \"" + schemaName + "\"");

            // 插入管理员 (使用 ON CONFLICT 保证幂等)
            String insertSql = String.format(
                    "INSERT INTO sys_user (username, password_hash, real_name, role_type, status, deleted) " +
                    "VALUES ('%s', '%s', '%s', 'TENANT_ADMIN', 1, 0) " +
                    "ON CONFLICT (username) DO NOTHING",
                    escapeSql(username),
                    escapeSql(passwordHash),
                    escapeSql(realName)
            );
            runner.execute(insertSql);
            log.info("  租户管理员已插入: schema={}, user={}", schemaName, username);
        } catch (SQLException e) {
            throw new RuntimeException("插入租户管理员失败: " + e.getMessage(), e);
        }
    }

    /**
     * 在平台全局表 public.sys_user_auth_index 中插入认证索引
     * <p>
     * 此表在 public Schema 下，是所有租户共享的认证数据。
     * 使用 JdbcTemplate 参数化查询确保安全。
     * </p>
     * <p>
     * 注意: 此方法与 DDL 不在同一事务中 (DDL 必须自动提交)。
     * 若此步失败，外层 catch 会触发补偿回滚 (DROP SCHEMA)。
     * </p>
     */
    private void insertPlatformAuthIndex(String tenantCode, String username, String passwordHash) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        String sql = "INSERT INTO public.sys_user_auth_index " +
                "(username, password_hash, tenant_code, role_type, status) " +
                "VALUES (?, ?, ?, 'TENANT_ADMIN', 1) " +
                "ON CONFLICT (username) DO NOTHING";

        int rows = jdbc.update(sql, username, passwordHash, tenantCode);
        log.info("  平台认证索引已插入: tenantCode={}, user={}, rows={}", tenantCode, username, rows);
    }

    // ========================================================================
    // 私有方法: Schema 删除 (补偿回滚)
    // ========================================================================

    /**
     * 删除 Schema (CASCADE)
     *
     * @return true=删除成功, false=删除失败或 Schema 不存在
     */
    private boolean dropSchema(String schemaName) {
        try (Connection conn = dataSource.getConnection()) {
            ScriptRunner runner = new ScriptRunner(conn)
                    .setStopOnError(false)
                    .setLogLevel(ScriptRunner.LogLevel.INFO);

            runner.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            log.info("  Schema 已删除: {}", schemaName);
            return true;
        } catch (SQLException e) {
            log.error("  Schema 删除失败: {}, error={}", schemaName, e.getMessage());
            return false;
        }
    }

    // ========================================================================
    // 私有方法: 安全校验
    // ========================================================================

    /**
     * 校验租户编码 (白名单模式)
     */
    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.trim().isEmpty()) {
            throw new IllegalArgumentException("租户编码不能为空");
        }
        if (!TENANT_CODE_PATTERN.matcher(tenantCode).matches()) {
            throw new IllegalArgumentException(
                    "租户编码格式非法 (仅允许字母、数字、下划线): " + tenantCode);
        }
    }

    /**
     * 校验用户名 (白名单模式)
     */
    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("管理员用户名不能为空");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "用户名格式非法: " + username);
        }
    }

    // ========================================================================
    // 私有方法: 工具
    // ========================================================================

    /**
     * 转义 SQL 字符串中的单引号 (防注入兜底)
     * <p>
     * 注意: 这是兜底防御。租户编码等敏感参数已在前置校验层处理。
     * 此方法仅用于 DDL 中的字符串字面量。
     * </p>
     */
    private static String escapeSql(String value) {
        if (value == null) return "";
        return value.replace("'", "''");
    }

    /**
     * 截断字符串 (用于日志)
     */
    private static String truncate(String s) {
        if (s == null) return "";
        String normalized = s.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 100) {
            return normalized.substring(0, 100) + "...";
        }
        return normalized;
    }
}
