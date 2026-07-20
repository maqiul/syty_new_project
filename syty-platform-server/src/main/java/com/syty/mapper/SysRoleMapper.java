package com.syty.mapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
/**
 * 角色 Mapper
 *
 * RBAC 核心: 提供用户角色的连表查询
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface SysRoleMapper extends BaseMapper<SysRoleEntity> {
    /**
     * 根据用户ID查询角色编码列表 (用于 Sa-Token StpInterface.getRoleList)
     *
     * 连表: sys_user_role -> sys_role
     * 只返回正常状态的角色
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 角色编码列表, 如 ["SUPER_ADMIN", "SHOP_ADMIN"]
     */
    @Select("SELECT r.role_code " +
            "FROM sys_user_role ur " +
            "JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND ur.tenant_id = #{tenantId} " +
            "  AND r.status = 1")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId,
                                          @Param("tenantId") Long tenantId);

    /**
     * 根据用户ID查询角色编码列表 (忽略租户过滤，用于获取超管角色等场景)
     */
    @Select("SELECT r.role_code " +
            "FROM sys_user_role ur " +
            "JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "  AND r.status = 1")
    List<String> selectRoleCodesByUserIdIgnoreTenant(@Param("userId") Long userId);
}
