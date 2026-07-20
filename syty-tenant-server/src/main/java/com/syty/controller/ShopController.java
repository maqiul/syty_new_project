package com.syty.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.Shop;
import com.syty.entity.ShopString;
import com.syty.entity.StringInfo;
import com.syty.entity.SysUserShop;
import com.syty.entity.Tenant;
import com.syty.mapper.SysUserShopMapper;
import com.syty.mapper.TenantMapper;
import com.syty.service.ShopService;
import com.syty.service.ShopStringService;
import com.syty.service.ShopTemplateSyncService;
import com.syty.service.StringInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "店铺管理")
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {
    private final ShopService shopService;
    private final ShopStringService shopStringService;
    private final StringInfoService stringInfoService;
    private final ShopTemplateSyncService shopTemplateSyncService;
    private final SysUserShopMapper sysUserShopMapper;
    private final TenantMapper tenantMapper;

    private void fillTenantNames(List<Shop> shops) {
        if (shops == null || shops.isEmpty()) return;
        Set<Long> tenantIds = new HashSet<>();
        for (Shop s : shops) {
            if (s.getTenantId() != null) tenantIds.add(s.getTenantId());
        }
        if (tenantIds.isEmpty()) return;
        List<Tenant> tenants = tenantMapper.selectBatchIds(tenantIds);
        Map<Long, String> nameMap = new HashMap<>();
        for (Tenant t : tenants) {
            nameMap.put(t.getId(), t.getName());
        }
        for (Shop s : shops) {
            s.setTenantName(nameMap.get(s.getTenantId()));
        }
    }

    @Operation(summary = "分页查询店铺")
    @GetMapping("/page")
    public Result<Page<Shop>> page(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Shop::getName, keyword);
        }
        wrapper.orderByDesc(Shop::getId);
        Page<Shop> result = shopService.page(new Page<>(page, size), wrapper);
        fillTenantNames(result.getRecords());
        return Result.success(result);
    }

    @Operation(summary = "店铺列表(下拉)")
    @GetMapping("/list")
    public Result<Object> list() {
        List<Shop> shops = shopService.list();
        fillTenantNames(shops);
        return Result.success(shops);
    }

    @Operation(summary = "获取店铺详情")
    @GetMapping("/{id}")
    public Result<Shop> get(@PathVariable Long id) {
        return Result.success(shopService.getById(id));
    }

    @Operation(summary = "新增店铺")
    @PostMapping
    public Result<Shop> add(@RequestBody Shop shop) {
        if (shop.getTenantId() == null) {
            shop.setTenantId(TenantContext.getTenantId());
        }
        boolean success = shopService.save(shop);
        if (success && shop.getId() != null) {
            // 新店创建成功后，自动同步公共打印模板和策略
            try {
                ShopTemplateSyncService.SyncResult syncResult = shopTemplateSyncService.syncPublicToShop(shop.getId());
                log.info("【店铺管理】店铺创建成功，同步模板策略完成: shopId={}, result={}", shop.getId(), syncResult);
            } catch (Exception e) {
                log.error("【店铺管理】店铺创建成功，但同步模板策略失败: shopId={}", shop.getId(), e);
            }
        }
        return Result.success(shop);
    }

    @Operation(summary = "更新店铺")
    @PutMapping
    public Result<Boolean> update(@RequestBody Shop shop) {
        return Result.success(shopService.updateById(shop));
    }

    @Operation(summary = "删除店铺")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(shopService.removeById(id));
    }

    @Operation(summary = "设置店铺穿线师")
    @PutMapping("/{shopId}/stringers")
    public Result<Boolean> setStringers(@PathVariable Long shopId, @RequestBody List<Long> stringerIds) {
        // 1. 删除旧关联
        LambdaQueryWrapper<ShopString> removeWrapper = new LambdaQueryWrapper<>();
        removeWrapper.eq(ShopString::getShopId, shopId);
        shopStringService.remove(removeWrapper);

        // 2. 添加新关联
        if (stringerIds != null && !stringerIds.isEmpty()) {
            List<ShopString> newList = stringerIds.stream().map(sid -> {
                ShopString ss = new ShopString();
                ss.setShopId(shopId);
                ss.setStringId(sid); // 假设 ShopString 有 stringId 字段
                return ss;
            }).collect(Collectors.toList());
            shopStringService.saveBatch(newList);
        }
        return Result.success(true);
    }

    @Operation(summary = "获取店铺可用线材")
    @GetMapping("/{shopId}/strings")
    public Result<List<StringInfo>> getStrings(@PathVariable Long shopId,
                                               @RequestParam(required = false) String keyword) {
        // 1. 查该店铺绑定了哪些线材 ID
        LambdaQueryWrapper<ShopString> shopStringWrapper = new LambdaQueryWrapper<>();
        shopStringWrapper.eq(ShopString::getShopId, shopId);
        List<ShopString> shopStrings = shopStringService.list(shopStringWrapper);
        
        Set<Long> stringIds = shopStrings.stream().map(ShopString::getStringId).collect(Collectors.toSet());
        
        // 2. 查线材详情
        LambdaQueryWrapper<StringInfo> wrapper = new LambdaQueryWrapper<>();
        if (!stringIds.isEmpty()) {
            wrapper.in(StringInfo::getId, stringIds);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(StringInfo::getBrand, keyword).or().like(StringInfo::getModel, keyword);
        }
        // StringInfo 没有 status 字段，不查 status
        return Result.success(stringInfoService.list(wrapper));
    }

    @Operation(summary = "获取当前租户所有店铺 ID")
    @GetMapping("/ids")
    public Result<Set<Long>> getAllShopIds() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return Result.success(new HashSet<>());
        }
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getTenantId, tenantId);
        List<Shop> shops = shopService.list(wrapper);
        return Result.success(shops.stream().map(Shop::getId).collect(Collectors.toSet()));
    }

    @Operation(summary = "获取指定用户关联的店铺")
    @GetMapping("/user/{userId}")
    public Result<List<Shop>> getUserShops(@PathVariable Long userId) {
        if (userId == null) return Result.success(new ArrayList<>());
        LambdaQueryWrapper<SysUserShop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserShop::getUserId, userId);
        List<SysUserShop> userShopList = sysUserShopMapper.selectList(wrapper);
        List<Long> shopIds = userShopList.stream().map(SysUserShop::getShopId).collect(Collectors.toList());
        if (shopIds.isEmpty()) return Result.success(new ArrayList<>());
        
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.in(Shop::getId, shopIds);
        List<Shop> shops = shopService.list(shopWrapper);
        fillTenantNames(shops);
        return Result.success(shops);
    }
}
