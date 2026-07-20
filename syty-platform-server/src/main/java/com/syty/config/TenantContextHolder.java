package com.syty.config;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

public class TenantContextHolder {
    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }

    @Component
    public static class TenantInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String uri = request.getRequestURI();
            
            // 平台端接口：不设置租户ID，保持为 null (查全局角色/权限)
            if (uri.startsWith("/api/platform") || uri.startsWith("/api/menu")) {
                return true;
            }

            // 租户端接口：从 Header 或参数获取
            String tenantHeader = request.getHeader("X-Tenant-Id");
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                try {
                    TENANT_ID_HOLDER.set(Long.parseLong(tenantHeader));
                    return true;
                } catch (NumberFormatException e) { }
            }

            String tenantParam = request.getParameter("tenantId");
            if (tenantParam != null && !tenantParam.isBlank()) {
                try {
                    TENANT_ID_HOLDER.set(Long.parseLong(tenantParam));
                    return true;
                } catch (NumberFormatException e) { }
            }

            // 租户端兜底
            TENANT_ID_HOLDER.set(1L);
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            clear();
        }
    }
}
