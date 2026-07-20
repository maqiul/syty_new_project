package com.syty.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户拦截器
 * 
 * 从请求 Header 中提取租户编码，并设置到 TenantContextHolder。
 * 登录接口通常不需要此拦截器设置 Context，因为登录时还不知道租户或由 Controller 处理。
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String HEADER_TENANT_CODE = "X-Tenant-Code";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantCode = request.getHeader(HEADER_TENANT_CODE);
        if (tenantCode != null && !tenantCode.isBlank()) {
            TenantContextHolder.setTenantCode(tenantCode);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 必须清理 ThreadLocal，防止线程池复用导致的数据串扰
        TenantContextHolder.clear();
    }
}
