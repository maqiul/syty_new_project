package com.syty.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.StockLog;
import com.syty.service.StockLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 库存流水 Controller
 */
@Tag(name = "库存流水")
@Slf4j
@RestController
@RequestMapping("/api/stock-log")
@RequiredArgsConstructor
public class StockLogController {

    private final StockLogService stockLogService;

    @SaCheckPermission("stock:view")
    @Operation(summary = "分页查询库存流水")
    @GetMapping("/page")
    public Result<Page<StockLog>> page(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) Long shopId,
                                        @RequestParam(required = false) Long stringId,
                                        @RequestParam(required = false) String changeType,
                                        @RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<StockLog> wrapper = new LambdaQueryWrapper<>();
        
        // 租户隔离
        if (!TenantContext.isSuperAdmin()) {
            wrapper.eq(StockLog::getTenantId, TenantContext.getTenantId());
        }
        
        // 店铺过滤
        if (shopId != null) {
            wrapper.eq(StockLog::getShopId, shopId);
        }
        
        // 线材过滤
        if (stringId != null) {
            wrapper.eq(StockLog::getStringId, stringId);
        }
        
        // 变动类型过滤
        if (StringUtils.hasText(changeType)) {
            wrapper.eq(StockLog::getChangeType, changeType);
        }
        
        // 日期范围
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(StockLog::getCreatedAt, startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(StockLog::getCreatedAt, endDate + " 23:59:59");
        }
        
        wrapper.orderByDesc(StockLog::getId);
        Page<StockLog> result = stockLogService.page(new Page<>(page, size), wrapper);
        return Result.success(result);
    }
}
