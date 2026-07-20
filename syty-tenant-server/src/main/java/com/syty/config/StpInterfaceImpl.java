package com.syty.config;
import cn.dev33.satoken.annotation.SaCheckPermission;
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
 *
 * 这是 RBAC 的核心: 每次鉴权时, Sa-Token 会调用此类获取用户的角色和权限列表.
 * 采用连表查询, 一次 SQL 拿到数据, 配合 Redis 缓存 (Sa-Token 自动缓存).
 *
 * 调用链路:
 * @SaCheckPermission("printer:config:view")
 *   → StpUtil.checkPermission("printer:config:view")
 *     → StpInterface.getPermissionList(loginId, loginType)
 *       → 查询 sys_user_role → sys_role_menu → sys_menu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    /**
     * 获取指定账号的权限列表
     *
     * 连表查询: sys_user_role -> sys_role_menu -> sys_menu
     * Sa-Token 会自动缓存结果 (默认 30 分钟, 可通过 satoken.timeout 配置)
     *
     * @param loginId 登录账号 ID (即用户ID)
     * @param loginType 登录类型 (忽略)
     * @return 权限标识列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        if (userId == null) {
            log.warn("获取权限列表失败: 无效的 loginId={}", loginId);
            return new ArrayList<>();
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            // 兜底: 如果没有租户上下文, 默认租户 1
            tenantId = 1L;
            log.debug("租户上下文为空, 使用默认租户: tenantId={}", tenantId);
        }
        List<String> permissions = sysMenuMapper.selectPermissionsByUserId(userId, tenantId);
        log.info("用户 userId={} 权限列表加载成功，共 {} 个权限: {}", userId, permissions.size(), permissions);
        return permissions;
    }
    /**
     * 获取指定账号的角色列表
     *
     * 连表查询: sys_user_role -> sys_role
     *
     * @param loginId 登录账号 ID (即用户ID)
     * @param loginType 登录类型 (忽略)
     * @return 角色编码列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        if (userId == null) {
            log.warn("获取角色列表失败: 无效的 loginId={}", loginId);
            return new ArrayList<>();
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = 1L;
            log.debug("租户上下文为空, 使用默认租户: tenantId={}", tenantId);
        }
        List<String> roles = sysRoleMapper.selectRoleCodesByUserId(userId, tenantId);
        log.debug("用户 userId={} 角色列表: {}", userId, roles);
        return roles;
    }
    /**
     * 安全解析 userId, 支持 String 和 Long 类型
     */
    private Long parseUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        if (loginId instanceof Long) {
            return (Long) loginId;
        }
        try {
            return Long.parseLong(loginId.toString());
        } catch (NumberFormatException e) {
            log.error("无法解析 userId: loginId={}", loginId, e);
            return null;
        }
    }
}
