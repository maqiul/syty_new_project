package com.syty.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.ShopString;
import com.syty.service.ShopStringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 网球店铺球线库存（兼容层）
 * V1.1 羽网合并后，网球线材库存已统一到 shop_string 表
 * 通过关联 string_info.sport_type='TENNIS' 区分
 *
 * @deprecated 后续版本将删除，前端应迁移到 /api/shop-string?sportType=TENNIS
 */
@Deprecated
@Tag(name = "网球店铺球线库存（兼容层）")
@RestController
@RequestMapping("/api/tennis/shop-string")
@RequiredArgsConstructor
public class TennisShopStringController {

    private final ShopStringService shopStringService;

    @GetMapping("/shop/{shopId}")
    @Operation(summary = "查询店铺网球线库存")
    public Result<Object> list(@PathVariable Long shopId) {
        // 查询该店铺下所有网球线材的库存
        // 通过 JOIN string_info 过滤 sportType='TENNIS'
        // 简化实现：直接查 shop_string，前端根据 string_info 的 sportType 过滤
        return Result.success(shopStringService.lambdaQuery()
                .eq(ShopString::getShopId, shopId)
                .list());
    }

    @SaCheckPermission("shop:string:create")
    @PostMapping
    @Operation(summary = "添加")
    public Result<Void> add(@RequestBody ShopString s) {
        s.setTenantId(TenantContext.getTenantId());
        shopStringService.save(s);
        return Result.success();
    }

    @SaCheckPermission("shop:string:edit")
    @PutMapping
    @Operation(summary = "修改")
    public Result<Void> update(@RequestBody ShopString s) {
        shopStringService.updateById(s);
        return Result.success();
    }

    @SaCheckPermission("shop:string:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shopStringService.removeById(id);
        return Result.success();
    }
}
