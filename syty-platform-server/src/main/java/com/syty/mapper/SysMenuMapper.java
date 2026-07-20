package com.syty.mapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

/**
 * 菜单 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 获取所有可见菜单 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_menu WHERE hidden = 0 ORDER BY sort_order")
    List<SysMenu> selectAllVisible();

    /** 获取角色的菜单ID列表 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_code = #{roleCode}")
    List<Long> selectMenuIdsByRole(@Param("roleCode") String roleCode);

    /** 获取用户的权限标识列表 (带租户隔离) */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT DISTINCT p.code " +
            "FROM sys_user_role ur " +
            "JOIN sys_role r ON r.id = ur.role_id " +
            "JOIN sys_role_menu rm ON rm.role_code = r.role_code " +
            "JOIN sys_menu m ON m.id = rm.menu_id " +
            "LEFT JOIN sys_permission p ON p.menu_id = m.id " +
            "WHERE ur.user_id = #{userId} AND ur.tenant_id = #{tenantId} " +
            "  AND p.code IS NOT NULL AND m.hidden = 0")
    List<String> selectPermissionsByUserId(@Param("userId") Long userId,
                                            @Param("tenantId") Long tenantId);

    /** 获取用户的权限标识列表 (忽略租户隔离，用于平台超管) */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT DISTINCT p.code " +
            "FROM sys_user_role ur " +
            "JOIN sys_role r ON r.id = ur.role_id " +
            "JOIN sys_role_menu rm ON rm.role_code = r.role_code " +
            "JOIN sys_menu m ON m.id = rm.menu_id " +
            "LEFT JOIN sys_permission p ON p.menu_id = m.id " +
            "WHERE ur.user_id = #{userId} AND ur.tenant_id IS NULL " +
            "  AND p.code IS NOT NULL AND m.hidden = 0")
    List<String> selectPermissionsByUserIdIgnoreTenant(@Param("userId") Long userId);

    /** 获取用户的菜单列表 (用于前端菜单树) */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT DISTINCT m.id, m.parent_id, m.name AS menu_name, m.path, m.component, m.icon, '' AS permission, CASE WHEN m.type = 0 THEN 'DIRECTORY' WHEN m.type = 1 THEN 'MENU' ELSE 'BUTTON' END AS menu_type, '' AS api_path, '' AS api_method, m.sort_order FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id JOIN sys_role_menu rm ON rm.role_code = r.role_code JOIN sys_menu m ON m.id = rm.menu_id WHERE ur.user_id = #{userId} AND m.hidden = 0 ORDER BY m.sort_order ASC")
    List<Map<String, Object>> selectMenusByUserId(@Param("userId") Long userId,
                                                    @Param("tenantId") Long tenantId);
}
