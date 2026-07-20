package com.syty.mapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 权限 Mapper
 * 核心修复：sys_permission 表无 tenant_id 字段，全局忽略租户拦截
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据角色编码查询权限列表
     * 核心修复：忽略租户拦截，防止插件在 sys_role_permission 表上追加不存在的 tenant_id 字段导致报错
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT rp.permission_code FROM sys_role_permission rp " +
            "JOIN sys_role r ON r.role_code = rp.role_code " +
            "WHERE r.role_code = #{roleCode}")
    List<String> selectCodesByRole(@Param("roleCode") String roleCode);
}
