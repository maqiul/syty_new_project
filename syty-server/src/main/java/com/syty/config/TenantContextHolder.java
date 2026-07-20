package com.syty.config;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
/**
 * 租户上下文持有者
 *
 * 通过 ThreadLocal 存储当前请求的租户 ID，供 MyBatis-Plus 租户插件使用。
 *
 * 租户 ID 获取优先级:
 * 1. Header: X-Tenant-Id (推荐, 微服务间调用)
 * 2. Parameter: tenantId (向后兼容)
 * 3. 默认: 1L (兜底, 单租户模式)
 */
public class TenantContextHolder {
    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();
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
    /**
     * 清除当前租户 ID (防止内存泄漏)
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }
    /**
     * Spring MVC 拦截器: 从请求中提取租户 ID
     */
    @Component
    public static class TenantInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // 优先从 Header 获取
            String tenantHeader = request.getHeader("X-Tenant-Id");
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                try {
                    TENANT_ID_HOLDER.set(Long.parseLong(tenantHeader));
                    return true;
                } catch (NumberFormatException e) {
                    // 忽略非法格式，走默认逻辑
                }
            }
            // 从请求参数获取
            String tenantParam = request.getParameter("tenantId");
            if (tenantParam != null && !tenantParam.isBlank()) {
                try {
                    TENANT_ID_HOLDER.set(Long.parseLong(tenantParam));
                    return true;
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
            // 默认租户 1
            TENANT_ID_HOLDER.set(1L);
            return true;
        }
        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            clear();
        }
    }
}
