package com.syty.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * SQL 脚本执行器
 * <p>
 * 基于 MyBatis ScriptRunner 理念简化实现，专为租户 Schema 初始化场景设计。
 * 支持批量 DDL/DML 执行、语句过滤、错误策略控制。
 * </p>
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li>默认 {@code stopOnError = true} — DDL 失败必须中断，不能静默跳过</li>
 *   <li>默认 {@code fullLineDelimiter = false} — 以分号 (;) 作为语句分隔符</li>
 *   <li>自动处理 PostgreSQL 的 NOTICE/WARNING 级别消息</li>
 * </ul>
 *
 * <h3>单点故障</h3>
 * <ul>
 *   <li>DDL 语句 (CREATE TABLE 等) 无法被事务回滚，调用方需自行处理补偿</li>
 *   <li>建议在调用方设置自动提交模式 (autoCommit=true) 以兼容 DDL</li>
 * </ul>
 */
@Slf4j
public class ScriptRunner {

    /** SQL 语句分隔符 */
    private static final String DEFAULT_DELIMITER = ";";

    /** 注释模式: 匹配 -- 开头的单行注释 */
    private static final Pattern LINE_COMMENT = Pattern.compile("^\\s*--.*$", Pattern.MULTILINE);

    /** 空行/纯空白行 */
    private static final Pattern BLANK_LINE = Pattern.compile("^\\s*$");

    private final Connection connection;

    /** 是否遇到错误时停止执行 (默认 true) */
    private boolean stopOnError = true;

    /** 是否使用整行作为分隔符 (默认 false，使用分号) */
    private boolean fullLineDelimiter = false;

    /** 日志输出级别: INFO / DEBUG / NONE */
    private LogLevel logLevel = LogLevel.INFO;

    /** 语句过滤器 (可选) — 过滤掉不需要的语句 */
    private java.util.function.Predicate<String> statementFilter = null;

    /**
     * 日志级别
     */
    public enum LogLevel {
        /** 只输出错误 */
        ERROR,
        /** 输出每条执行的 SQL */
        INFO,
        /** 输出每条 SQL 及其结果 */
        DEBUG,
        /** 静默模式 */
        NONE
    }

    /**
     * 构造函数
     *
     * @param connection 数据库连接 (调用方负责生命周期管理)
     */
    public ScriptRunner(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }

    /**
     * 设置遇到错误时是否停止执行
     */
    public ScriptRunner setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
        return this;
    }

    /**
     * 设置是否使用整行作为分隔符
     */
    public ScriptRunner setFullLineDelimiter(boolean fullLineDelimiter) {
        this.fullLineDelimiter = fullLineDelimiter;
        return this;
    }

    /**
     * 设置日志级别
     */
    public ScriptRunner setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    /**
     * 设置语句过滤器
     * <p>
     * 过滤器接收完整 SQL 语句 (不含分隔符)，返回 true 表示执行，false 表示跳过。
     * 典型用法: 过滤掉包含特定 schema 前缀的语句。
     * </p>
     *
     * @param filter 过滤谓词
     */
    public ScriptRunner setStatementFilter(java.util.function.Predicate<String> filter) {
        this.statementFilter = filter;
        return this;
    }

    /**
     * 从 Reader 读取并执行 SQL 脚本
     *
     * @param reader SQL 脚本输入流
     * @throws SQLException SQL 执行异常 (当 stopOnError=true 时)
     * @throws IOException  读取异常
     */
    public void runScript(Reader reader) throws SQLException, IOException {
        boolean originalAutoCommit;
        try {
            originalAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            throw new SQLException("Failed to get autoCommit status", e);
        }

        // DDL 需要自动提交
        try {
            if (!originalAutoCommit) {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.warn("Failed to set autoCommit=true, proceeding anyway", e);
        }

        try {
            executeReader(reader);
        } finally {
            // 恢复原始 autoCommit 状态
            try {
                if (!originalAutoCommit) {
                    connection.setAutoCommit(false);
                }
            } catch (SQLException e) {
                log.warn("Failed to restore autoCommit=false", e);
            }
        }
    }

    /**
     * 执行单条 SQL 语句 (便捷方法)
     *
     * @param sql SQL 语句
     * @throws SQLException 执行异常
     */
    public void execute(String sql) throws SQLException {
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }

        // 应用过滤器
        if (statementFilter != null && !statementFilter.test(sql.trim())) {
            if (logLevel == LogLevel.DEBUG) {
                log.debug("[ScriptRunner] FILTERED (skipped): {}", truncateSql(sql));
            }
            return;
        }

        if (logLevel == LogLevel.INFO || logLevel == LogLevel.DEBUG) {
            log.info("[ScriptRunner] EXECUTING: {}", truncateSql(sql));
        }

        try (Statement stmt = connection.createStatement()) {
            boolean hasResultSet = stmt.execute(sql);

            if (hasResultSet && logLevel == LogLevel.DEBUG) {
                try (ResultSet rs = stmt.getResultSet()) {
                    int rowCount = 0;
                    if (rs != null) {
                        while (rs.next()) {
                            rowCount++;
                        }
                    }
                    log.debug("[ScriptRunner] ResultSet rows: {}", rowCount);
                }
            }

            // 检查 warnings
            SQLWarning warning = stmt.getWarnings();
            if (warning != null && logLevel != LogLevel.NONE) {
                log.warn("[ScriptRunner] SQL Warning: {}", warning.getMessage());
            }
        } catch (SQLException e) {
            if (stopOnError) {
                log.error("[ScriptRunner] FAILED: {}", truncateSql(sql));
                log.error("[ScriptRunner] Error: {}", e.getMessage());
                throw e;
            } else {
                log.warn("[ScriptRunner] FAILED (continuing): {} — {}", truncateSql(sql), e.getMessage());
            }
        }
    }

    /**
     * 读取并解析 Reader 中的 SQL 语句
     */
    private void executeReader(Reader reader) throws SQLException, IOException {
        StringBuilder currentStatement = new StringBuilder();
        boolean inDollarQuoted = false;

        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                // 跳过注释和空行
                String trimmed = line.trim();
                if (BLANK_LINE.matcher(trimmed).matches() || trimmed.startsWith("--")) {
                    continue;
                }

                // 检测 PostgreSQL 的 $$ 美元引号块 (用于函数体)
                if (trimmed.contains("$$")) {
                    inDollarQuoted = !inDollarQuoted;
                }

                currentStatement.append(line).append("\n");

                // 判断语句结束
                if (!inDollarQuoted) {
                    if (fullLineDelimiter) {
                        // 整行作为一条语句
                        String stmt = currentStatement.toString().trim();
                        if (!stmt.isEmpty()) {
                            execute(stmt);
                        }
                        currentStatement.setLength(0);
                    } else if (trimmed.endsWith(DEFAULT_DELIMITER)) {
                        // 分号分隔
                        // 去掉末尾分号 (execute() 不需要)
                        String stmt = currentStatement.toString().trim();
                        if (stmt.endsWith(DEFAULT_DELIMITER)) {
                            stmt = stmt.substring(0, stmt.length() - 1).trim();
                        }
                        if (!stmt.isEmpty()) {
                            execute(stmt);
                        }
                        currentStatement.setLength(0);
                    }
                }
            }

            // 处理文件末尾残留的语句 (没有以分号结尾)
            String remaining = currentStatement.toString().trim();
            if (!remaining.isEmpty() && !BLANK_LINE.matcher(remaining).matches()) {
                // 去掉末尾分号如果存在
                if (remaining.endsWith(DEFAULT_DELIMITER)) {
                    remaining = remaining.substring(0, remaining.length() - 1).trim();
                }
                if (!remaining.isEmpty()) {
                    execute(remaining);
                }
            }
        }
    }

    /**
     * 截断 SQL 用于日志输出 (防止日志爆炸)
     */
    private static String truncateSql(String sql) {
        if (sql == null) return "";
        String normalized = sql.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 120) {
            return normalized.substring(0, 120) + "...";
        }
        return normalized;
    }
}
