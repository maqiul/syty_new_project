package com.syty.config;
import cn.dev33.satoken.stp.StpInterface;
import com.syty.mapper.SysMenuMapper;
import com.syty.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限加载器实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        if (userId == null) {
            log.warn("获取权限列表失败: 无效的 loginId={}", loginId);
            return new ArrayList<>();
        }
        Long tenantId = TenantContextHolder.getTenantId();
        
        List<String> permissions;
        if (tenantId == null) {
            permissions = sysMenuMapper.selectPermissionsByUserIdIgnoreTenant(userId);
            log.info("平台用户 userId={} 权限加载 (全局), 共 {} 个", userId, permissions.size());
        } else {
            permissions = sysMenuMapper.selectPermissionsByUserId(userId, tenantId);
            log.info("租户用户 userId={} 权限加载 (tenantId={}), 共 {} 个", userId, tenantId, permissions.size());
        }
        return permissions;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        if (userId == null) {
            log.warn("获取角色列表失败: 无效的 loginId={}", loginId);
            return new ArrayList<>();
        }
        Long tenantId = TenantContextHolder.getTenantId();
        
        List<String> roles;
        if (tenantId == null) {
            roles = sysRoleMapper.selectRoleCodesByUserIdIgnoreTenant(userId);
            log.info("平台用户 userId={} 角色加载 (全局): {}", userId, roles);
        } else {
            roles = sysRoleMapper.selectRoleCodesByUserId(userId, tenantId);
            log.info("租户用户 userId={} 角色加载 (tenantId={}): {}", userId, tenantId, roles);
        }
        return roles;
    }

    private Long parseUserId(Object loginId) {
        if (loginId == null) return null;
        if (loginId instanceof Long) return (Long) loginId;
        try { return Long.parseLong(loginId.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}
