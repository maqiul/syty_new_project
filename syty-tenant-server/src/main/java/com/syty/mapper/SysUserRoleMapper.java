package com.syty.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.SysUserRoleEntity;
import org.apache.ibatis.annotations.Mapper;
/**
 * 用户-角色关联 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRoleEntity> {
}
