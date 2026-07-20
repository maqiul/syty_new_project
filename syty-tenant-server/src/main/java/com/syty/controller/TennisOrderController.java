package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.TennisOrder;
import com.syty.entity.Player;
import com.syty.entity.Racket;
import com.syty.service.TennisOrderService;
import com.syty.service.PlayerService;
import com.syty.service.RacketService;
import com.syty.service.StockService;
import com.syty.util.SnowflakeIdGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
@Tag(name = "网球订单管理")
@RestController
@RequestMapping("/api/tennis/order")
@RequiredArgsConstructor
public class TennisOrderController {
    private final TennisOrderService orderService;
    private final PlayerService playerService;
    private final RacketService racketService;
    private final StockService stockService;
    private final com.syty.util.SnowflakeIdGenerator snowflakeIdGenerator;
    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<Page<TennisOrder>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) Long shopId,
                                          @RequestParam(required = false) Integer status) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<TennisOrder> w = new LambdaQueryWrapper<>();
        w.eq(TennisOrder::getTenantId, tenantId);
        if (shopId != null) w.eq(TennisOrder::getShopId, shopId);
        if (status != null) w.eq(TennisOrder::getStatus, status);
        w.orderByDesc(TennisOrder::getId);
        return Result.success(orderService.page(new Page<>(page, size), w));
    }
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "创建订单")
    public Result<TennisOrder> create(@RequestBody TennisOrder order) {
        order.setTenantId(TenantContext.getTenantId());
        order.setStatus(0);
        // 使用雪花算法生成订单号
        order.setOrderNo("SO" + snowflakeIdGenerator.nextId());
        orderService.save(order);
        
        // 库存预留
        if (order.getMainStringId() != null) {
            stockService.reserveStock(order.getShopId(), order.getMainStringId(), 1, order.getId(), order.getOrderNo());
        }
        if (order.getCrossStringId() != null) {
            stockService.reserveStock(order.getShopId(), order.getCrossStringId(), 1, order.getId(), order.getOrderNo());
        }
        
        return Result.success(order);
    }
    @PutMapping
    @Operation(summary = "修改订单")
    public Result<Void> update(@RequestBody TennisOrder order) { orderService.updateById(order); return Result.success(); }
    @PutMapping("/{id}/complete")
    @Operation(summary = "完成穿线")
    public Result<Void> complete(@PathVariable Long id) {
        TennisOrder o = orderService.getById(id);
        if (o == null) return Result.error("订单不存在");
        
        // 1. 扣减库存 (实扣，释放预留并扣除实物)
        if (o.getMainStringId() != null) {
            stockService.deductStock(o.getShopId(), o.getMainStringId(), 1, o.getId(), o.getOrderNo());
        }
        if (o.getCrossStringId() != null) {
            stockService.deductStock(o.getShopId(), o.getCrossStringId(), 1, o.getId(), o.getOrderNo());
        }
        
        // 2. 更新状态
        TennisOrder update = new TennisOrder(); 
        update.setId(id); 
        update.setStatus(1); // 1 = Completed
        orderService.updateById(update);
        
        // 3. 触发打印等逻辑 (此处略)
        
        return Result.success();
    }
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public Result<Void> cancel(@PathVariable Long id) {
        TennisOrder o = orderService.getById(id);
        if (o != null && o.getStatus() == 0) {
            // 释放库存
            if (o.getMainStringId() != null) {
                stockService.releaseStock(o.getShopId(), o.getMainStringId(), 1, o.getId(), o.getOrderNo());
            }
            if (o.getCrossStringId() != null) {
                stockService.releaseStock(o.getShopId(), o.getCrossStringId(), 1, o.getId(), o.getOrderNo());
            }
            // 更新状态
            o.setStatus(-1); // -1 = Cancelled
            orderService.updateById(o);
            return Result.success();
        }
        return Result.error("订单状态不允许取消");
    }
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { orderService.removeById(id); return Result.success(); }
    @Operation(summary = "客户自助登记网球订单（公开接口")
    @PostMapping("/customer")
    public Result<Object> customerRegister(@RequestBody Map<String, Object> params) {
        // 参数提取
        String phone = params.get("phone") != null ? params.get("phone").toString() : null;
        String name = params.get("name") != null ? params.get("name").toString() : null;
        String racketModel = params.get("racketModel") != null ? params.get("racketModel").toString() : null;
        Long shopId = params.get("shopId") != null ? Long.valueOf(params.get("shopId").toString()) : null;
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        if (phone == null || name == null || shopId == null) {
            return Result.error("手机号、姓名和店铺为必填项");
        }
        // 查找或创建球员
        Long playerId = null;
        if (phone != null) {
            Player existing = playerService.getOne(
                    new LambdaQueryWrapper<Player>().eq(Player::getPhone, phone).last("LIMIT 1"));
            if (existing != null) {
                playerId = existing.getId();
            } else {
                Player newPlayer = new Player();
                newPlayer.setName(name);
                newPlayer.setPhone(phone);
                playerService.save(newPlayer);
                playerId = newPlayer.getId();
            }
        }
        // 查找或创建球员
        Long racketId = null;
        if (StringUtils.hasText(racketModel)) {
            Racket existing = racketService.getOne(
                    new LambdaQueryWrapper<Racket>().eq(Racket::getModel, racketModel).last("LIMIT 1"));
            if (existing != null) {
                racketId = existing.getId();
            } else {
                Racket newRacket = new Racket();
                newRacket.setBrand(racketModel.split(" ")[0]);
                newRacket.setModel(racketModel);
                racketService.save(newRacket);
                racketId = newRacket.getId();
            }
        }
        // 创建订单
        // 使用雪花算法生成订单号 (替换原有的 时间戳+随机数 方案)
        String orderNo = "SO" + snowflakeIdGenerator.nextId();
        TennisOrder order = new TennisOrder();
        order.setOrderNo(orderNo);
        order.setShopId(shopId);
        order.setPlayerId(playerId);
        order.setRacketId(racketId);
        order.setPlayerPhone(phone);
        order.setRemark(remark);
        order.setStatus(0);
        order.setTenantId(1L); // 公开接口默认租户1，后续可根据域名/参数动态化
        orderService.save(order);
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("id", order.getId());
        return Result.success(result);
    }
}
