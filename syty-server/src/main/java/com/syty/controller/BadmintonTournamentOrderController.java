package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.BadmintonTournamentOrder;
import com.syty.service.BadmintonTournamentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@Tag(name = "羽毛球大赛订")
@RestController
@RequestMapping("/api/badminton-tournament")
@RequiredArgsConstructor
public class BadmintonTournamentOrderController {
    private final BadmintonTournamentOrderService service;
    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result<Page<BadmintonTournamentOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<BadmintonTournamentOrder> w = new LambdaQueryWrapper<>();
        Long tid = TenantContext.getTenantId();
        if (tid != null) w.eq(BadmintonTournamentOrder::getTenantId, tid);
        w.orderByDesc(BadmintonTournamentOrder::getId);
        return Result.success(service.page(new Page<>(page, size), w));
    }
    @Operation(summary = "新增")
    @PostMapping
    public Result<BadmintonTournamentOrder> add(@RequestBody BadmintonTournamentOrder order) {
        Long tid = TenantContext.getTenantId();
        order.setTenantId(tid != null ? tid : 1L);
        order.setOrderNo("BT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        service.save(order);
        return Result.success(order);
    }
    @Operation(summary = "修改")
    @PutMapping
    public Result<Void> update(@RequestBody BadmintonTournamentOrder order) {
        service.updateById(order);
        return Result.success();
    }
    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.success();
    }
}
