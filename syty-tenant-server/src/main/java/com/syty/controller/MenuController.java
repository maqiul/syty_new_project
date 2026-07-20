package com.syty.controller;
import com.syty.common.Result;
import com.syty.entity.SysMenu;
import com.syty.mapper.SysMenuMapper;
import com.syty.mapper.SysRoleMenuMapper;
import com.syty.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {
    private final SysMenuService menuService;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    /** 获取当前用户可见的菜单树 */
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> getMenuTreeByRole(@RequestParam String roleCode) {
        return Result.success(menuService.getMenuTreeByRole(roleCode));
    }
    /** 管理员获取全部菜单树 */
    @GetMapping("/all")
    public Result<List<Map<String, Object>>> getAllMenus() {
        return Result.success(menuService.getMenuTree());
    }
    /** 添加菜单 */
    @PostMapping
    public Result<SysMenu> add(@RequestBody SysMenu menu) {
        menu.setId(null);
        menuService.save(menu);
        return Result.success(menu);
    }
    /** 更新菜单 */
    @PutMapping("/{id}")
    public Result<SysMenu> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.updateById(menu);
        return Result.success(menu);
    }
    /** 删除菜单 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.removeById(id);
        return Result.success();
    }
    /** 获取角色的菜单ID列表 */
    @GetMapping("/role/{roleCode}")
    public Result<List<Long>> getRoleMenus(@PathVariable String roleCode) {
        return Result.success(menuMapper.selectMenuIdsByRole(roleCode));
    }
    /** 设置角色菜单 */
    @PutMapping("/role/{roleCode}")
    public Result<Void> setRoleMenus(@PathVariable String roleCode, @RequestBody List<Long> menuIds) {
        roleMenuMapper.deleteByRole(roleCode);
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMenuMapper.batchInsert(roleCode, menuIds);
        }
        return Result.success();
    }
}
