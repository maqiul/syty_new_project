package com.syty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.SysUserAuthIndex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 管理员认证索引 Mapper
 * 
 * 查询 public.sys_user_auth_index 表（位于 public schema，不受租户插件影响）。
 * SQL 中显式写死 schema 名，确保始终查询 public schema。
 */
@Mapper
public interface SysUserAuthIndexMapper extends BaseMapper<SysUserAuthIndex> {

    /**
     * 根据用户名查询管理员（直接查 public schema）
     */
    @Select("SELECT id, tenant_code, username, password, real_name, phone, email, status, deleted, created_at, updated_at " +
            "FROM public.sys_user_auth_index " +
            "WHERE username = #{username} AND deleted = 0")
    SysUserAuthIndex selectByUsername(@Param("username") String username);
}
