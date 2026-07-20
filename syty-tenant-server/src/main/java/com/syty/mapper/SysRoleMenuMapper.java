package com.syty.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.SysRoleMenu;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 角色-菜单关联 Mapper
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    @Delete("DELETE FROM sys_role_menu WHERE role_code = #{roleCode}")
    void deleteByRole(@Param("roleCode") String roleCode);

    @Insert("<script>" +
            "INSERT INTO sys_role_menu (role_code, menu_id) VALUES " +
            "<foreach collection='menuIds' item='menuId' separator=','>" +
            "(#{roleCode}, #{menuId})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("roleCode") String roleCode, @Param("menuIds") List<Long> menuIds);
}
