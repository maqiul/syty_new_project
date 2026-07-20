package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
/**
 * 系统菜单/权限实体
 *
 * 对应表: sys_menu
 * 字段: id, parent_id, menu_type, name, permission, path, api_path, api_method, sort_order, status, tenant_id
 */
@Data
@TableName("sys_menu")
public class SysMenuEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 父菜单ID (0=根节点) */
    private Long parentId;
    /** 类型: MENU-菜单, BUTTON-按钮, API-接口权限 */
    private String menuType;
    /** 菜单/权限名称 (DB 列名: name) */
    @TableField("name")
    private String menuName;
    /** 权限标识 (如 printer:config:view) */
    private String permission;
    /** 路由路径 (菜单用) */
    private String path;
    /** API路径 (如 /api/v1/print-setup) */
    private String apiPath;
    /** HTTP方法: GET/POST/PUT/DELETE */
    private String apiMethod;
    /** 排序号 */
    private Integer sortOrder;
    /** 状态: 0-停用, 1-正常 */
    private Integer status;
    /** 租户ID */
    private Long tenantId;
}
