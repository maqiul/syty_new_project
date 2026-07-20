package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.common.Result;
import com.syty.entity.SysPermission;
import com.syty.entity.SysRolePermission;
import com.syty.mapper.SysPermissionMapper;
import com.syty.mapper.SysRolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    /** 全部权限列表（扁平） */
    @GetMapping
    public Result<List<SysPermission>> list() {
        return Result.success(permissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSortOrder)));
    }
    /** 权限字*/
    @GetMapping("/tree")
    public Result<List<SysPermission>> tree() {
        List<SysPermission> all = permissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSortOrder));
        return Result.success(buildTree(all, ""));
    }
    private List<SysPermission> buildTree(List<SysPermission> all, String parentCode) {
        List<SysPermission> result = new ArrayList<>();
        for (SysPermission p : all) {
            String pc = p.getParentCode() == null ? "" : p.getParentCode();
            if (!pc.equals(parentCode)) continue;
            List<SysPermission> children = buildTree(all, p.getCode());
            if (!children.isEmpty()) {
                p.setChildren(children);
            }
            result.add(p);
        }
        return result;
    }
    /** 新增权限 */
    @PostMapping
    public Result<SysPermission> add(@RequestBody SysPermission perm) {
        perm.setId(null);
        if (perm.getParentCode() != null && perm.getParentCode().isEmpty()) {
            perm.setParentCode(null);
        }
        permissionMapper.insert(perm);
        return Result.success(perm);
    }
    /** 更新权限 */
    @PutMapping("/{id}")
    public Result<SysPermission> update(@PathVariable Long id, @RequestBody SysPermission perm) {
        perm.setId(id);
        if (perm.getParentCode() != null && perm.getParentCode().isEmpty()) {
            perm.setParentCode(null);
        }
        permissionMapper.updateById(perm);
        return Result.success(perm);
    }
    /** 删除权限 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permissionMapper.deleteById(id);
        return Result.success();
    }
    /** 获取角色权限字*/
    @GetMapping("/role/{roleCode}/codes")
    public Result<List<String>> getRolePermissionCodes(@PathVariable String roleCode) {
        List<String> codes = permissionMapper.selectCodesByRole(roleCode);
        return Result.success(codes);
    }
    /** 设置角色权限 */
    @PutMapping("/role/{roleCode}")
    public Result<Void> setRolePermissions(@PathVariable String roleCode, @RequestBody List<String> codes) {
        rolePermissionMapper.delete(
            new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleCode, roleCode));
        if (codes != null && !codes.isEmpty()) {
            for (String code : codes) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleCode(roleCode);
                rp.setPermissionCode(code);
                rolePermissionMapper.insert(rp);
            }
        }
        return Result.success();
    }
}
