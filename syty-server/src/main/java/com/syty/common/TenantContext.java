package com.syty.common;
import java.util.List;
/**
 * 租户上下文- 存储当前请求的租户信息
 * 通过 ThreadLocal 实现请求级别的租户隔离
 */
public class TenantContext {
    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ROLE = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> CURRENT_PERMISSIONS = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ENABLE_BADMINTON_TOURNAMENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ENABLE_TENNIS_TOURNAMENT = new ThreadLocal<>();
    public static void setTenantId(Long tenantId) { CURRENT_TENANT.set(tenantId); }
    public static Long getTenantId() { return CURRENT_TENANT.get(); }
    public static boolean hasTenantId() { return CURRENT_TENANT.get() != null; }
    public static void setRole(String role) { CURRENT_ROLE.set(role); }
    public static String getRole() { return CURRENT_ROLE.get(); }
    public static void setUserId(Long userId) { CURRENT_USER_ID.set(userId); }
    public static Long getUserId() { return CURRENT_USER_ID.get(); }
    public static void setUsername(String username) { CURRENT_USERNAME.set(username); }
    public static String getUsername() { return CURRENT_USERNAME.get(); }
    public static void setPermissions(List<String> permissions) { CURRENT_PERMISSIONS.set(permissions); }
    public static List<String> getPermissions() { return CURRENT_PERMISSIONS.get(); }
    public static boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(CURRENT_ROLE.get());
    }
    /**
     * 检查当前用户是否拥有指定权限
     */
    public static boolean hasPermission(String permissionCode) {
        List<String> permissions = CURRENT_PERMISSIONS.get();
        if (permissions == null) return false;
        return permissions.contains(permissionCode);
    }
    // ===== 大赛功能开关=====
    public static void setEnableBadmintonTournament(Boolean v) { ENABLE_BADMINTON_TOURNAMENT.set(v); }
    public static boolean isEnableBadmintonTournament() { return Boolean.TRUE.equals(ENABLE_BADMINTON_TOURNAMENT.get()); }
    public static void setEnableTennisTournament(Boolean v) { ENABLE_TENNIS_TOURNAMENT.set(v); }
    public static boolean isEnableTennisTournament() { return Boolean.TRUE.equals(ENABLE_TENNIS_TOURNAMENT.get()); }
    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_ROLE.remove();
        CURRENT_USER_ID.remove();
        CURRENT_USERNAME.remove();
        CURRENT_PERMISSIONS.remove();
        ENABLE_BADMINTON_TOURNAMENT.remove();
        ENABLE_TENNIS_TOURNAMENT.remove();
    }
}
