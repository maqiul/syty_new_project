package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
/**
 * 角色-菜单关联实体
 *
 * 对应表: sys_role_menu
 * 联合主键: (role_id, menu_id, tenant_id), 无自增 id
 * 字段: role_id, menu_id, tenant_id
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenuEntity {
    /** 角色ID (联合主键之一) */
    private Long roleId;
    /** 菜单/权限ID (联合主键之一) */
    private Long menuId;
    /** 租户ID (联合主键之一) */
    private Long tenantId;
}
