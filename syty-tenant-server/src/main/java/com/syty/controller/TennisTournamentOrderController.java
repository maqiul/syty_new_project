package com.syty.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.TennisTournamentOrder;
import com.syty.service.TennisTournamentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@Tag(name = "网球大赛订单")
@RestController
@RequestMapping("/api/tennis/tournament")
@RequiredArgsConstructor
public class TennisTournamentOrderController {
    private final TennisTournamentOrderService service;
    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result<Page<TennisTournamentOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<TennisTournamentOrder> w = new LambdaQueryWrapper<>();
        Long tid = TenantContext.getTenantId();
        if (tid != null) w.eq(TennisTournamentOrder::getTenantId, tid);
        w.orderByDesc(TennisTournamentOrder::getId);
        return Result.success(service.page(new Page<>(page, size), w));
    }
    @SaCheckPermission("tennis:tournament:create")
    @Operation(summary = "新增")
    @PostMapping
    public Result<TennisTournamentOrder> add(@RequestBody TennisTournamentOrder order) {
        Long tid = TenantContext.getTenantId();
        order.setTenantId(tid != null ? tid : 1L);
        order.setOrderNo("TT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        service.save(order);
        return Result.success(order);
    }
    @SaCheckPermission("tennis:tournament:edit")
    @Operation(summary = "修改")
    @PutMapping
    public Result<Void> update(@RequestBody TennisTournamentOrder order) {
        service.updateById(order);
        return Result.success();
    }
    @SaCheckPermission("tennis:tournament:delete")
    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.success();
    }
}
