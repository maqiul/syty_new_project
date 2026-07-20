package com.syty.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
/**
 * 菜单树节点 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuTreeVO {
    /** 菜单ID */
    private Long id;
    /** 父菜单ID */
    private Long parentId;
    /** 类型: MENU-菜单, BUTTON-按钮, API-接口权限 */
    private String menuType;
    /** 菜单/权限名称 -> 前端路由 name 字段 */
    @JsonProperty("name")
    private String menuName;
    /** 权限标识 (如 printer:config:view) */
    private String permission;
    /** 路由路径 (菜单用) */
    private String path;
    /** 前端组件名 (如 Dashboard, User) */
    private String component;
    /** 图标名称 */
    private String icon;
    /** API路径 */
    private String apiPath;
    /** HTTP方法 */
    private String apiMethod;
    /** 排序号 */
    private Integer sortOrder;
    /** 子菜单列表 */
    private List<MenuTreeVO> children;
}
