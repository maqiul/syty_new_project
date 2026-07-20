package com.syty.config;

/**
 * 租户上下文持有者
 * 
 * 双模式支持：
 * 1. tenantCode (String) — 用于动态切换 PostgreSQL Schema（Schema 隔离）
 * 2. tenantId (Long)    — 用于 MyBatis-Plus 行级租户隔离插件
 * 
 * 使用场景：
 * - 管理员登录：clear() 清除所有上下文，走 public schema
 * - 员工登录：setTenantCode(tenantCode) + setTenantId(tenantId)，走 tenant_xxx schema
 */
public class TenantContextHolder {
    
    /**
     * 租户编码 (对应 PostgreSQL Schema 名称，如 tenant_acme)
     */
    private static final ThreadLocal<String> TENANT_CODE_HOLDER = new ThreadLocal<>();
    
    /**
     * 租户 ID (用于行级隔离)
     */
    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();

    // ========== tenantCode 相关 ==========

    /**
     * 设置当前租户编码
     */
    public static void setTenantCode(String tenantCode) {
        TENANT_CODE_HOLDER.set(tenantCode);
    }

    /**
     * 获取当前租户编码
     */
    public static String getTenantCode() {
        return TENANT_CODE_HOLDER.get();
    }

    // ========== tenantId 相关 ==========

    /**
     * 设置当前租户 ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    /**
     * 获取当前租户 ID
     */
    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    // ========== 清除 ==========

    /**
     * 清除当前租户上下文 (请求结束时必须调用，防止线程池复用导致的数据泄漏)
     */
    public static void clear() {
        TENANT_CODE_HOLDER.remove();
        TENANT_ID_HOLDER.remove();
    }
}
