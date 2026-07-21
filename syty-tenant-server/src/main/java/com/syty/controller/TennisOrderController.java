package com.syty.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.StringingOrder;
import com.syty.service.StringingOrderService;
import com.syty.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 网球订单管理（兼容层）
 * V1.1 羽网合并后，网球订单已统一到 stringing_order 表（sportType='TENNIS'）
 *
 * @deprecated 后续版本将删除，前端应迁移到 /api/order?sportType=TENNIS
 */
@Deprecated
@Tag(name = "网球订单管理（兼容层）")
@RestController
@RequestMapping("/api/tennis/order")
@RequiredArgsConstructor
public class TennisOrderController {

    private final StringingOrderService orderService;
    private final StockService stockService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<Page<StringingOrder>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) Long shopId,
                                              @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<StringingOrder> w = new LambdaQueryWrapper<>();
        w.eq(StringingOrder::getSportType, "TENNIS");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            w.eq(StringingOrder::getTenantId, tenantId);
        }
        if (shopId != null) w.eq(StringingOrder::getShopId, shopId);
        if (status != null) w.eq(StringingOrder::getStatus, status);
        w.orderByDesc(StringingOrder::getId);
        return Result.success(orderService.page(new Page<>(page, size), w));
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "创建订单")
    public Result<StringingOrder> create(@RequestBody StringingOrder order) {
        order.setSportType("TENNIS");
        order.setTenantId(TenantContext.getTenantId());
        order.setStatus(0);
        if (order.getOrderNo() == null || order.getOrderNo().isBlank()) {
            order.setOrderNo("SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + (new Random().nextInt(9000) + 1000));
        }
        orderService.save(order);

        // 库存预留
        try {
            if (order.getMainStringId() != null) {
                stockService.reserveStock(order.getShopId(), order.getMainStringId(), 1, order.getId(), order.getOrderNo());
            }
            if (order.getCrossStringId() != null) {
                stockService.reserveStock(order.getShopId(), order.getCrossStringId(), 1, order.getId(), order.getOrderNo());
            }
        } catch (Exception e) {
            // 预留失败不阻断接单
        }

        return Result.success(order);
    }

    @PutMapping
    @Operation(summary = "修改订单")
    public Result<Void> update(@RequestBody StringingOrder order) {
        orderService.updateById(order);
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "完成穿线")
    public Result<Void> complete(@PathVariable Long id) {
        StringingOrder o = orderService.getById(id);
        if (o == null) return Result.error("订单不存在");

        // 扣减库存
        if (o.getMainStringId() != null) {
            stockService.deductStock(o.getShopId(), o.getMainStringId(), 1, o.getId(), o.getOrderNo());
        }
        if (o.getCrossStringId() != null) {
            stockService.deductStock(o.getShopId(), o.getCrossStringId(), 1, o.getId(), o.getOrderNo());
        }

        StringingOrder update = new StringingOrder();
        update.setId(id);
        update.setStatus(2); // 2 = 已完成
        orderService.updateById(update);

        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public Result<Void> cancel(@PathVariable Long id) {
        StringingOrder o = orderService.getById(id);
        if (o != null && o.getStatus() == 0) {
            if (o.getMainStringId() != null) {
                stockService.releaseStock(o.getShopId(), o.getMainStringId(), 1, o.getId(), o.getOrderNo());
            }
            if (o.getCrossStringId() != null) {
                stockService.releaseStock(o.getShopId(), o.getCrossStringId(), 1, o.getId(), o.getOrderNo());
            }
            StringingOrder update = new StringingOrder();
            update.setId(id);
            update.setStatus(-1);
            orderService.updateById(update);
            return Result.success();
        }
        return Result.error("订单状态不允许取消");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.removeById(id);
        return Result.success();
    }
}
