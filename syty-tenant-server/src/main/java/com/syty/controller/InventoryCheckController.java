package com.syty.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.InventoryCheck;
import com.syty.entity.InventoryCheckItem;
import com.syty.service.InventoryCheckService;
import com.syty.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant/inventory-check")
@RequiredArgsConstructor
public class InventoryCheckController {

    private final InventoryCheckService checkService;

    @PostMapping
    public Result<InventoryCheck> createCheck(@RequestParam(required = false) Long shopId) {
        Long tenantId = TenantContext.getTenantId() != null ? Long.valueOf(TenantContext.getTenantId()) : 1L;
        return Result.success(checkService.createCheck(tenantId, shopId));
    }

    @GetMapping
    public Result<IPage<InventoryCheck>> pageCheck(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        Long tenantId = TenantContext.getTenantId() != null ? Long.valueOf(TenantContext.getTenantId()) : 1L;
        return Result.success(checkService.pageCheck(new Page<>(page, size), tenantId));
    }

    @GetMapping("/{id}/items")
    public Result<List<InventoryCheckItem>> getCheckItems(@PathVariable Long id) {
        return Result.success(checkService.getCheckItems(id));
    }

    @PutMapping("/{id}/items")
    public Result<Void> submitItems(@PathVariable Long id, @RequestBody List<InventoryCheckItem> items) {
        checkService.submitItems(id, items);
        return Result.success(null);
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirmCheck(@PathVariable Long id) {
        checkService.confirmCheck(id);
        return Result.success(null);
    }
}
