package com.syty.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.BizException;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.SysUser;
import com.syty.entity.Tenant;
import com.syty.mapper.SysUserMapper;
import com.syty.mapper.TenantMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class SysUserController {
    private final SysUserMapper sysUserMapper;
    private final TenantMapper tenantMapper;
    private boolean isSuperAdmin() {
        return TenantContext.isSuperAdmin();
    }
    private String currentUserRole() {
        return TenantContext.getRole();
    }
    private void checkRoleManagePermission(String targetRole) {
        if (isSuperAdmin()) return;
        if (!"STAFF".equals(targetRole)) {
            throw BizException.forbidden("您只能管理普通员工（STAFF）角");
        }
    }
    private void checkSameTenant(Long targetTenantId) {
        if (isSuperAdmin()) return;
        Long myTenantId = TenantContext.getTenantId();
        if (myTenantId == null || !myTenantId.equals(targetTenantId)) {
            throw BizException.forbidden("您只能管理本租户的用");
        }
    }
    private void checkCanDelete(SysUser target) {
        Long currentUserId = TenantContext.getUserId();
        if (currentUserId != null && currentUserId.equals(target.getId())) {
            throw BizException.of("不能删除自己");
        }
        if ("SUPER_ADMIN".equals(target.getRole())) {
            long superAdminCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "SUPER_ADMIN"));
            if (superAdminCount <= 1) {
                throw BizException.of("不能删除唯一的超级管理员");
            }
        }
    }
    private void checkCanChangeRole(SysUser target, String newRole) {
        Long currentUserId = TenantContext.getUserId();
        if (currentUserId != null && currentUserId.equals(target.getId())) {
            throw BizException.forbidden("不能修改自己的角");
        }
        if ("SUPER_ADMIN".equals(target.getRole()) && !"SUPER_ADMIN".equals(newRole)) {
            long superAdminCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "SUPER_ADMIN"));
            if (superAdminCount <= 1) {
                throw BizException.of("不能将唯一的超级管理员降级");
            }
        }
    }
    @Operation(summary = "分页查询用户")
    @SaCheckPermission("user:page")
    @GetMapping("/page")
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword);
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        // 非超管只看本租户
        if (!isSuperAdmin()) {
            wrapper.eq(SysUser::getTenantId, TenantContext.getTenantId());
        }
        wrapper.orderByDesc(SysUser::getId);
        return Result.success(sysUserMapper.selectPage(new Page<>(page, size), wrapper));
    }
    @Operation(summary = "新增用户")
    @SaCheckPermission("user:create")
    @PostMapping
    public Result<Void> add(@RequestBody SysUser user) {
        checkRoleManagePermission(user.getRole());
        // 校验租户（非超管字
        if (!isSuperAdmin() && user.getTenantId() != null) {
            checkSameTenant(user.getTenantId());
        }
        // 校验用户名是否已存在（按租户隔离字
        LambdaQueryWrapper<SysUser> checkWrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername());
        if (user.getTenantId() != null) {
            // 租户用户：检查同租户下是否重字
            checkWrapper.eq(SysUser::getTenantId, user.getTenantId());
        } else {
            // 超管：检查全局是否重名（tenantId 字null字
            checkWrapper.isNull(SysUser::getTenantId);
        }
        SysUser existing = sysUserMapper.selectOne(checkWrapper);
        if (existing != null) {
            return Result.error("用户名已存在");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        user.setStatus(1);
        sysUserMapper.insert(user);
        return Result.success();
    }
    @Operation(summary = "修改用户")
    @SaCheckPermission("user:edit")
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        SysUser existing = sysUserMapper.selectById(user.getId());
        if (existing == null) {
            return Result.error("用户不存");
        }
        // 权限校验
        checkSameTenant(existing.getTenantId());
        if (user.getRole() != null && !user.getRole().equals(existing.getRole())) {
            checkCanChangeRole(existing, user.getRole());
            checkRoleManagePermission(user.getRole());
        }
        // 密码不为空则加密
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(BCrypt.hashpw(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        sysUserMapper.updateById(user);
        return Result.success();
    }
    @Operation(summary = "删除用户")
    @SaCheckPermission("user:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存");
        }
        checkSameTenant(user.getTenantId());
        checkCanDelete(user);
        sysUserMapper.deleteById(id);
        return Result.success();
    }
    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存");
        }
        checkSameTenant(user.getTenantId());
        user.setPassword(BCrypt.hashpw("123456"));
        sysUserMapper.updateById(user);
        return Result.success();
    }
    @Operation(summary = "获取当前用户")
    @GetMapping("/me")
    public Result<Object> me() {
        return Result.success(sysUserMapper.selectById(TenantContext.getUserId()));
    }
    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        String oldPwd = params.get("oldPassword");
        String newPwd = params.get("newPassword");
        if (!StringUtils.hasText(oldPwd) || !StringUtils.hasText(newPwd)) {
            return Result.error("新旧密码均不能为");
        }
        Long uid = TenantContext.getUserId();
        SysUser user = sysUserMapper.selectById(uid);
        if (user == null) return Result.error("用户不存");
        if (!BCrypt.checkpw(oldPwd, user.getPassword())) {
            return Result.error("原密码错");
        }
        user.setPassword(BCrypt.hashpw(newPwd));
        sysUserMapper.updateById(user);
        StpUtil.logout(uid);
        return Result.success();
    }
    @Operation(summary = "获取租户下用户列")
    @GetMapping("/list")
    public Result<Object> list(@RequestParam(required = false) String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (!isSuperAdmin()) {
            wrapper.eq(SysUser::getTenantId, TenantContext.getTenantId());
        }
        return Result.success(sysUserMapper.selectList(wrapper));
    }
    @Operation(summary = "转移用户到新租户")
    @PutMapping("/{id}/transfer-tenant")
    public Result<Void> transferTenant(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Long newTenantId = Long.valueOf(params.get("tenantId").toString());
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) return Result.error("用户不存");
        if (!isSuperAdmin()) {
            throw BizException.forbidden("您不能将用户转移到其他租");
        }
        Tenant tenant = tenantMapper.selectById(newTenantId);
        if (tenant == null) return Result.error("目标租户不存");
        user.setTenantId(newTenantId);
        sysUserMapper.updateById(user);
        return Result.success();
    }
}
