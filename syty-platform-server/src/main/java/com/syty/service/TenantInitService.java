package com.syty.service;

/**
 * 租户自动化初始化服务 (V1.9)
 * 
 * <h3>职责</h3>
 * <p>
 * 新租户注册时自动完成环境初始化，按顺序执行:
 * </p>
 * <ol>
 *   <li>检查并创建独立 Schema (tenant_{code})</li>
 *   <li>读取 DDL 模板脚本，过滤 public 语句后在租户 Schema 下执行</li>
 *   <li>插入种子数据 (租户管理员账号)</li>
 *   <li>在平台全局表 (public.sys_user_auth_index) 注册认证索引</li>
 * </ol>
 * 
 * <h3>架构决策</h3>
 * <ul>
 *   <li><b>隔离策略</b>: 共享 PostgreSQL 实例，每个租户独立 Schema</li>
 *   <li><b>DDL 执行</b>: 通过 ScriptRunner 逐条执行 DDL，自动提交</li>
 *   <li><b>回滚策略</b>: DDL 不可事务回滚，采用补偿性回滚 (DROP SCHEMA CASCADE)</li>
 *   <li><b>安全</b>: 租户编码白名单校验 + 参数化查询，防止 SQL 注入</li>
 * </ul>
 * 
 * <h3>单点故障</h3>
 * <ul>
 *   <li>执行期间平台宕机 → Schema 可能残留，需手动 {@link #cleanupTenant(String)}</li>
 *   <li>建议在 tenant 表增加 init_status 字段做状态标记 (TODO: V2.0)</li>
 * </ul>
 * 
 * @author 老K (架构师)
 * @since V1.9
 */
public interface TenantInitService {

    /**
     * 初始化新租户的独立 Schema 环境
     *
     * @param tenantCode     租户编码 (用于 schema 命名: tenant_{code}, 仅允许字母/数字/下划线)
     * @param adminUsername  管理员用户名 (将同时作为登录凭证)
     * @param adminPassword  管理员明文密码 (方法内部 BCrypt 加密)
     * @param adminRealName  管理员真实姓名
     * @throws TenantInitException 初始化失败时抛出，包含补偿回滚状态
     */
    void initTenant(String tenantCode,
                    String adminUsername, String adminPassword,
                    String adminRealName);

    /**
     * 清理指定租户的 Schema (手动补偿工具)
     * <p>
     * 用于 initTenant() 失败后的残留清理，或租户注销时的数据清理。
     * </p>
     *
     * @param tenantCode 租户编码
     * @return true=清理成功, false=schema 不存在
     */
    boolean cleanupTenant(String tenantCode);

    /**
     * 检查指定租户的 Schema 是否已存在
     *
     * @param tenantCode 租户编码
     * @return true=已存在, false=不存在
     */
    boolean schemaExists(String tenantCode);

    /**
     * 租户初始化异常
     * <p>
     * 携带回滚结果信息，调用方可据此决定是否需要人工介入。
     * </p>
     */
    class TenantInitException extends RuntimeException {
        private final String tenantCode;
        private final boolean rollbackSucceeded;

        public TenantInitException(String tenantCode, String message, Throwable cause, boolean rollbackSucceeded) {
            super(String.format("[租户=%s] %s", tenantCode, message), cause);
            this.tenantCode = tenantCode;
            this.rollbackSucceeded = rollbackSucceeded;
        }

        public String getTenantCode() {
            return tenantCode;
        }

        /**
         * @return 补偿回滚是否成功
         */
        public boolean isRollbackSucceeded() {
            return rollbackSucceeded;
        }
    }
}
