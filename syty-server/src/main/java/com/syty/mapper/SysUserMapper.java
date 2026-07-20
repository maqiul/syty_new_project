package com.syty.mapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT id, tenant_id, username, password, real_name, phone, status, deleted, created_at, updated_at " +
            "FROM sys_user " +
            "WHERE username = #{username} AND tenant_id = #{tenantId} AND deleted = 0")
    SysUser selectByUsername(@Param("username") String username, @Param("tenantId") Long tenantId);

    /**
     * 查询平台超管 (忽略租户插件)
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id, tenant_id, username, password, real_name, phone, status, deleted, created_at, updated_at " +
            "FROM sys_user " +
            "WHERE username = #{username} AND tenant_id IS NULL AND deleted = 0")
    SysUser selectSuperAdmin(@Param("username") String username);
}
