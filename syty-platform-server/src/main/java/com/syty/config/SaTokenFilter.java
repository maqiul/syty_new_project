package com.syty.config;
import cn.dev33.satoken.stp.StpUtil;
import com.syty.common.TenantContext;
import com.syty.service.SysPermissionService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.util.List;
/**
 * Sa-Token 上下文过滤器
 * 从TokenSession 中提取用户信息到 TenantContext ThreadLocal
 * 支持 X-Tenant-Id 请求头覆盖（超管切换租户视角）
 */
@Component
@Order(-100)
@RequiredArgsConstructor
public class SaTokenFilter implements Filter {
    private final SysPermissionService sysPermissionService;
    private final com.syty.service.TenantCacheService tenantCacheService;
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String path = request.getRequestURI();
        // 只处理API 请求
        if (path.startsWith("/api/")) {
            try {
                // 已登录则从Sa-Token 会话中提取用户信息
                if (StpUtil.isLogin()) {
                    Long userId = StpUtil.getLoginIdAsLong();
                    // 从TokenSession 中读取
                    Object roleObj = StpUtil.getTokenSession().get("role");
                    String role = roleObj != null ? roleObj.toString() : null;
                    Object tenantIdObj = StpUtil.getTokenSession().get("tenantId");
                    Long tenantId = tenantIdObj != null
                            ? Long.valueOf(tenantIdObj.toString()) : null;
                    Object usernameObj = StpUtil.getTokenSession().get("username");
                    String username = usernameObj != null ? usernameObj.toString() : null;
                    if (role != null) TenantContext.setRole(role);
                    if (tenantId != null && tenantId > 0) TenantContext.setTenantId(tenantId);
                    TenantContext.setUserId(userId);
                    if (username != null) TenantContext.setUsername(username);
                    // 加载权限列表
                    @SuppressWarnings("unchecked")
                    List<String> permissions = (List<String>) StpUtil.getTokenSession().get("permissions");
                    if (permissions == null && role != null) {
                        // 兜底：直接从数据库加载
                        permissions = sysPermissionService.getPermissionCodesByRole(role);
                    }
                    TenantContext.setPermissions(permissions);
                    // 大赛功能开关（优先 Session，兜底Redis 缓存，变更即时生效））
                    Object btmObj = StpUtil.getTokenSession().get("enableBadmintonTournament");
                    Object ttmObj = StpUtil.getTokenSession().get("enableTennisTournament");
                    if (btmObj != null && ttmObj != null) {
                        TenantContext.setEnableBadmintonTournament((Boolean) btmObj);
                        TenantContext.setEnableTennisTournament((Boolean) ttmObj);
                    } else if (tenantId != null) {
                        var flags = tenantCacheService.getTournamentFlags(tenantId);
                        if (flags != null) {
                            TenantContext.setEnableBadmintonTournament(flags.badminton());
                            TenantContext.setEnableTennisTournament(flags.tennis());
                        } else {
                            TenantContext.setEnableBadmintonTournament(true);
                            TenantContext.setEnableTennisTournament(false);
                        }
                    }
                }
                // X-Tenant-Id 请求头覆盖（超管切换租户视角）
                String headerTenantId = request.getHeader("X-Tenant-Id");
                if (StringUtils.hasText(headerTenantId)) {
                    try {
                        TenantContext.setTenantId(Long.parseLong(headerTenantId));
                    } catch (NumberFormatException ignored) {}
                }
                filterChain.doFilter(servletRequest, servletResponse);
            } finally {
                TenantContext.clear();
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}
