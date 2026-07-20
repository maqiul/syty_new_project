package com.syty.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.TenantInfo;
import com.syty.service.TenantInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 平台端 - 租户管理接口
 */
@Slf4j
@Tag(name = "平台端-租户管理", description = "仅 SUPER_ADMIN 可访问")
@RestController
@RequestMapping("/api/platform/tenants")
@RequiredArgsConstructor
public class PlatformTenantController {

    private final TenantInfoService tenantInfoService;
    private final com.syty.mapper.PackageInfoMapper packageMapper;
    private final com.syty.service.TenantPackageService tenantPackageService;

    /**
     * 分页查询租户列表
     */
    @Operation(summary = "分页查询租户")
    @SaCheckRole("SUPER_ADMIN")
    @GetMapping("/list")
    public Result<Page<TenantInfo>> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<TenantInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(TenantInfo::getName, keyword)
                    .or()
                    .like(TenantInfo::getTenantCode, keyword)
                    .or()
                    .like(TenantInfo::getContactPerson, keyword)
            );
        }
        wrapper.orderByDesc(TenantInfo::getCreatedAt);
        return Result.success(tenantInfoService.page(new Page<>(page, size), wrapper));
    }

    /**
     * 获取租户详情
     */
    @Operation(summary = "获取租户详情")
    @SaCheckRole("SUPER_ADMIN")
    @GetMapping("/{id}")
    public Result<TenantInfo> get(@PathVariable Long id) {
        TenantInfo tenant = tenantInfoService.getById(id);
        if (tenant == null) {
            return Result.error("租户不存在");
        }
        return Result.success(tenant);
    }

    /**
     * 新增租户
     */
    @Operation(summary = "新增租户")
    @SaCheckRole("SUPER_ADMIN")
    @PostMapping
    public Result<TenantInfo> add(@RequestBody TenantInfo tenant) {
        if (!StringUtils.hasText(tenant.getTenantCode())) {
            return Result.error("租户编码不能为空");
        }
        if (!StringUtils.hasText(tenant.getName())) {
            return Result.error("租户名称不能为空");
        }
        if (tenant.getPackageId() == null) {
            return Result.error("请选择所属套餐");
        }
        // 检查编码是否重复
        long count = tenantInfoService.count(
                new LambdaQueryWrapper<TenantInfo>().eq(TenantInfo::getTenantCode, tenant.getTenantCode()));
        if (count > 0) {
            return Result.error("租户编码已存在");
        }

        // 获取套餐信息
        com.syty.entity.PackageInfo pkg = packageMapper.selectById(tenant.getPackageId());
        if (pkg == null) {
            return Result.error("所选套餐不存在");
        }

        // 计算过期时间
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        tenant.setPackageExpiredAt(now.plusDays(pkg.getDurationDays()));
        tenant.setPackageStatus("ACTIVE");

        // 保存租户信息
        tenantInfoService.save(tenant);
        log.info("租户信息保存成功: id={}, code={}, packageId={}, expiredAt={}", 
                tenant.getId(), tenant.getTenantCode(), tenant.getPackageId(), tenant.getPackageExpiredAt());

        // 创建租户专属 Schema + 初始化表结构 + 种子数据
        try {
            tenantInfoService.createTenantSchema(tenant.getTenantCode());
            log.info("租户模式创建成功: code={}", tenant.getTenantCode());
        } catch (Exception e) {
            log.error("租户模式创建失败, 回滚租户信息: tenantCode={}", tenant.getTenantCode(), e);
            tenantInfoService.removeById(tenant.getId());
            return Result.error("租户模式初始化失败: " + e.getMessage());
        }

        return Result.success(tenant);
    }

    /**
     * 修改租户
     */
    @Operation(summary = "修改租户")
    @SaCheckRole("SUPER_ADMIN")
    @PutMapping
    public Result<Void> update(@RequestBody TenantInfo tenant) {
        if (tenant.getId() == null) {
            return Result.error("租户ID不能为空");
        }
        TenantInfo existing = tenantInfoService.getById(tenant.getId());
        if (existing == null) {
            return Result.error("租户不存在");
        }
        tenantInfoService.updateById(tenant);
        return Result.success();
    }

    /**
     * 删除租户（逻辑删除）
     */
    @Operation(summary = "删除租户")
    @SaCheckRole("SUPER_ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        TenantInfo existing = tenantInfoService.getById(id);
        if (existing == null) {
            return Result.error("租户不存在");
        }
        tenantInfoService.removeById(id);
        return Result.success();
    }

    /**
     * 租户续费/调整套餐
     */
    @Operation(summary = "租户续费/调整套餐")
    @SaCheckRole("SUPER_ADMIN")
    @PutMapping("/{id}/package")
    public Result<Void> renewPackage(@PathVariable Long id, 
                                     @RequestBody com.syty.dto.RenewPackageRequest request) {
        if (request.getPackageId() == null) {
            return Result.error("请选择套餐");
        }
        try {
            tenantPackageService.renewPackage(
                id, 
                request.getPackageId(), 
                request.getDurationDays(), 
                request.getOperateType(), 
                request.getRemark()
            );
            return Result.success();
        } catch (Exception e) {
            log.error("续费失败", e);
            return Result.error(e.getMessage());
        }
    }
}
