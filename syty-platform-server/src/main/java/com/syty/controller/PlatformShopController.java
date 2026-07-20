package com.syty.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.dto.ApiResult;
import com.syty.dto.ShopRenewRequest;
import com.syty.entity.PackageInfo;
import com.syty.entity.SysShop;
import com.syty.entity.TenantInfo;
import com.syty.mapper.PackageInfoMapper;
import com.syty.mapper.SysShopMapper;
import com.syty.mapper.TenantInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/platform/shops")
@RequiredArgsConstructor
public class PlatformShopController {

    private final SysShopMapper sysShopMapper;
    private final PackageInfoMapper packageInfoMapper;
    private final TenantInfoMapper tenantInfoMapper; // 注入租户 Mapper

    /**
     * 分页查询门店列表
     */
    @SaCheckRole("SUPER_ADMIN")
    @GetMapping("/list")
    public ApiResult<Page<SysShop>> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String keyword) {
        Page<SysShop> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysShop> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(SysShop::getShopName, keyword)
                    .or()
                    .like(SysShop::getShopCode, keyword)
            );
        }
        wrapper.orderByDesc(SysShop::getCreatedAt);
        
        sysShopMapper.selectPage(pageParam, wrapper);
        return ApiResult.success(pageParam);
    }

    /**
     * 新增门店
     * 自动生成门店编码，根据套餐计算到期时间。
     */
    @SaCheckRole("SUPER_ADMIN")
    @PostMapping
    public ApiResult<Void> add(@RequestBody Map<String, Object> params) {
        // 1. 参数校验
        String tenantCode = (String) params.get("tenantCode");
        String shopName = (String) params.get("shopName");
        Object packageIdObj = params.get("packageId");
        String shopCode = (String) params.get("shopCode");

        if (!StringUtils.hasText(tenantCode)) {
            return ApiResult.error("租户编码不能为空");
        }
        if (!StringUtils.hasText(shopName)) {
            return ApiResult.error("门店名称不能为空");
        }
        if (packageIdObj == null) {
            return ApiResult.error("套餐ID不能为空");
        }

        Long packageId = Long.valueOf(packageIdObj.toString());

        // 1.1 查询租户信息 (获取 tenantId)
        LambdaQueryWrapper<TenantInfo> tenantQuery = new LambdaQueryWrapper<>();
        tenantQuery.eq(TenantInfo::getTenantCode, tenantCode);
        TenantInfo tenant = tenantInfoMapper.selectOne(tenantQuery);
        
        if (tenant == null) {
            return ApiResult.error("租户不存在，编码: " + tenantCode);
        }

        // 2. 自动生成门店编码（格式：S + yyyyMMddHHmmss）
        if (!StringUtils.hasText(shopCode)) {
            shopCode = "S" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        }

        // 3. 查询套餐，计算到期时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt;
        PackageInfo pkg = packageInfoMapper.selectById(packageId);
        if (pkg != null && pkg.getDurationDays() != null && pkg.getDurationDays() > 0) {
            expiredAt = now.plusDays(pkg.getDurationDays());
            log.info("新增门店，套餐ID: {}, 套餐名称: {}, 有效期: {} 天", packageId, pkg.getName(), pkg.getDurationDays());
        } else {
            // 套餐不存在或未配置天数，默认 365 天
            expiredAt = now.plusDays(365);
            log.warn("新增门店，套餐ID: {} 不存在或未配置有效期，使用默认 365 天", packageId);
        }

        // 4. 构建实体并插入
        SysShop shop = new SysShop();
        shop.setTenantId(tenant.getId()); // 设置租户 ID
        shop.setTenantCode(tenantCode);
        shop.setShopName(shopName);
        shop.setShopCode(shopCode);
        shop.setPackageId(packageId);
        shop.setExpiredAt(expiredAt);
        shop.setStatus("ACTIVE");
        shop.setCreatedAt(now);
        shop.setUpdatedAt(now);

        sysShopMapper.insert(shop);
        log.info("新增门店成功: tenantCode={}, shopName={}, shopCode={}, expiredAt={}",
                tenantCode, shopName, shopCode, expiredAt);

        return ApiResult.success();
    }

    /**
     * 门店续费
     * 计费主体是门店，按门店 ID 续期。
     * 如果原 expiredAt > 当前时间，在 expiredAt 基础上加 days；
     * 如果原 expiredAt <= 当前时间，从当前时间起加 days。
     * 续费后若状态为过期/停用，自动恢复为 ACTIVE。
     */
    @SaCheckRole("SUPER_ADMIN")
    @PutMapping("/{id}/renew")
    public ApiResult<SysShop> renew(@PathVariable Long id,
                                     @RequestBody ShopRenewRequest request) {
        // 1. 查询门店
        SysShop shop = sysShopMapper.selectById(id);
        if (shop == null) {
            return ApiResult.error("门店不存在");
        }

        // 2. 确定目标套餐 (支持续费时换套餐/升降级)
        Long targetPackageId = request.getPackageId() != null ? request.getPackageId() : shop.getPackageId();
        
        // 如果换了套餐，更新门店的套餐 ID
        if (targetPackageId != null && !targetPackageId.equals(shop.getPackageId())) {
            shop.setPackageId(targetPackageId);
            log.info("门店续费时更换套餐: shopId={}, newPackageId={}", id, targetPackageId);
        }

        // 3. 确定续费天数：如果前端没传或<=0，自动读取套餐默认天数
        Integer days = request.getDays();
        if (days == null || days <= 0) {
            if (targetPackageId != null) {
                com.syty.entity.PackageInfo pkg = packageInfoMapper.selectById(targetPackageId);
                if (pkg != null && pkg.getDurationDays() != null) {
                    days = pkg.getDurationDays();
                }
            }
            // 如果套餐也没配置天数，兜底 365 天
            if (days == null || days <= 0) {
                days = 365;
            }
        }

        // 4. 计算新到期时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiredAt;
        if (shop.getExpiredAt() != null && shop.getExpiredAt().isAfter(now)) {
            // 尚未过期：在原到期时间基础上累加
            newExpiredAt = shop.getExpiredAt().plusDays(days);
        } else {
            // 已过期或未设置：从当前时间起算
            newExpiredAt = now.plusDays(days);
        }

        // 5. 更新数据库
        shop.setExpiredAt(newExpiredAt);
        shop.setUpdatedAt(now);

        // 续费后自动激活：如果当前状态是过期或停用，改为 ACTIVE
        if ("EXPIRED".equals(shop.getStatus()) || "SUSPENDED".equals(shop.getStatus())) {
            shop.setStatus("ACTIVE");
        }

        sysShopMapper.updateById(shop);
        log.info("门店续费成功: shopId={}, shopName={}, packageId={}, 新到期时间={}", 
                id, shop.getShopName(), shop.getPackageId(), newExpiredAt);

        return ApiResult.success(shop);
    }
}
