package com.syty.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.ShopString;
import com.syty.service.ShopStringService;
import com.syty.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenant/inventory/stock")
@RequiredArgsConstructor
public class StockWarningController {

    private final ShopStringService shopStringService;

    @SaCheckPermission("stock:view")
    @GetMapping
    public Result<IPage<ShopString>> pageStock(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(defaultValue = "false") boolean warningOnly) {
        Long tenantId = TenantContext.getTenantId() != null ? Long.valueOf(TenantContext.getTenantId()) : 1L;
        return Result.success(shopStringService.pageStock(new Page<>(page, size), warningOnly, tenantId));
    }
}
