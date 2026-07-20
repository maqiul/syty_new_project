package com.syty.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.Tenant;
import com.syty.service.TenantCacheService;
import com.syty.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
@Tag(name = "租户管理", description = "字SUPER_ADMIN 可访")
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantController {
    private final TenantService tenantService;
    private final TenantCacheService tenantCacheService;
    @Operation(summary = "分页查询租户")
    @SaCheckPermission("system:tenant:page")
    @GetMapping("/page")
    public Result<Page<Tenant>> page(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Tenant::getName, keyword).or().like(Tenant::getCode, keyword);
        }
        wrapper.orderByDesc(Tenant::getId);
        return Result.success(tenantService.page(new Page<>(page, size), wrapper));
    }
    @Operation(summary = "租户列表(下拉)")
    @GetMapping("/list")
    public Result<Object> list() {
        return Result.success(tenantService.list());
    }
    @Operation(summary = "获取租户详情")
    @GetMapping("/{id}")
    public Result<Tenant> get(@PathVariable Long id) {
        return Result.success(tenantService.getById(id));
    }
    @Operation(summary = "新增租户")
    @SaCheckPermission("system:tenant:create")
    @PostMapping
    public Result<Tenant> add(@RequestBody Tenant tenant) {
        Tenant existing = tenantService.lambdaQuery().eq(Tenant::getCode, tenant.getCode()).one();
        if (existing != null) {
            return Result.error("租户编码已存");
        }
        tenantService.save(tenant);
        return Result.success(tenant);
    }
    @Operation(summary = "修改租户")
    @SaCheckPermission("system:tenant:edit")
    @PutMapping
    public Result<Void> update(@RequestBody Tenant tenant) {
        tenantService.updateById(tenant);
        // 清除 Redis 缓存，使大赛开关立即生字
        tenantCacheService.evictTournamentFlags(tenant.getId());
        return Result.success();
    }
    @Operation(summary = "删除租户")
    @SaCheckPermission("system:tenant:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tenantService.removeById(id);
        tenantCacheService.evictTournamentFlags(id);
        return Result.success();
    }
}
