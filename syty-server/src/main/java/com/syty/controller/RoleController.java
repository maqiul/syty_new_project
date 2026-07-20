package com.syty.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.common.Result;
import com.syty.entity.SysRoleEntity;
import com.syty.mapper.SysRoleMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理 Controller
 * 路径简化为 /api/role，与其他 Controller 保持一致
 */
@Slf4j
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "系统角色增删改查")
public class RoleController {

    private final SysRoleMapper sysRoleMapper;

    @GetMapping("/list")
    @SaCheckPermission("system:role:page")
    @Operation(summary = "角色列表")
    public Result<List<SysRoleEntity>> list() {
        // 依赖 MyBatis-Plus 租户插件自动过滤
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysRoleEntity::getId);
        
        List<SysRoleEntity> list = sysRoleMapper.selectList(wrapper);
        log.info("查询角色列表, size={}", list.size());
        return Result.success(list);
    }

    @PostMapping("/add")
    @SaCheckPermission("system:role:create")
    @Operation(summary = "新增角色")
    public Result<Void> add(@RequestBody SysRoleEntity role) {
        sysRoleMapper.insert(role);
        log.info("新增角色成功: {}", role.getRoleName());
        return Result.success();
    }

    @PostMapping("/update")
    @SaCheckPermission("system:role:edit")
    @Operation(summary = "更新角色")
    public Result<Void> update(@RequestBody SysRoleEntity role) {
        if (role.getId() == null) return Result.error(400, "ID不能为空");
        sysRoleMapper.updateById(role);
        return Result.success();
    }

    @PostMapping("/delete")
    @SaCheckPermission("system:role:delete")
    @Operation(summary = "删除角色")
    public Result<Void> delete(@RequestParam Long id) {
        sysRoleMapper.deleteById(id);
        return Result.success();
    }
}
