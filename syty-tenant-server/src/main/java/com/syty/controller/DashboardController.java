package com.syty.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.*;
import com.syty.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "工作台统计")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StringingOrderMapper orderMapper;
    private final PlayerMapper playerMapper;
    private final ShopMapper shopMapper;
    private final TenantMapper tenantMapper;

    @Operation(summary = "获取工作台统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new LinkedHashMap<>();

        if (TenantContext.isSuperAdmin()) {
            // 超管看平台级统计
            LocalDate today = LocalDate.now();
            LocalDateTime todayStart = today.atStartOfDay();
            LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

            // 今日订单（羽+网统一）
            LambdaQueryWrapper<StringingOrder> orderW = new LambdaQueryWrapper<>();
            orderW.ge(StringingOrder::getCreatedAt, todayStart)
                  .le(StringingOrder::getCreatedAt, todayEnd);
            Long todayOrders = orderMapper.selectCount(orderW);

            // 待处理订单
            LambdaQueryWrapper<StringingOrder> pendingW = new LambdaQueryWrapper<>();
            pendingW.eq(StringingOrder::getStatus, 0);
            Long pendingOrders = orderMapper.selectCount(pendingW);

            // 球员总数（羽+网统一）
            Long totalPlayers = playerMapper.selectCount(null);

            // 租户总数
            Long totalTenants = tenantMapper.selectCount(null);

            data.put("todayOrders", todayOrders);
            data.put("pendingOrders", pendingOrders);
            data.put("totalPlayers", totalPlayers);
            data.put("totalTenants", totalTenants);
        } else {
            // 租户用户看租户内统计
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return Result.error(400, "未绑定租户");
            }

            LocalDate today = LocalDate.now();
            LocalDateTime todayStart = today.atStartOfDay();
            LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

            // 今日订单（羽+网统一）
            LambdaQueryWrapper<StringingOrder> orderW = new LambdaQueryWrapper<>();
            orderW.eq(StringingOrder::getTenantId, tenantId)
                  .ge(StringingOrder::getCreatedAt, todayStart)
                  .le(StringingOrder::getCreatedAt, todayEnd);
            Long todayOrders = orderMapper.selectCount(orderW);

            // 待处理订单
            LambdaQueryWrapper<StringingOrder> pendingW = new LambdaQueryWrapper<>();
            pendingW.eq(StringingOrder::getTenantId, tenantId)
                    .eq(StringingOrder::getStatus, 0);
            Long pendingOrders = orderMapper.selectCount(pendingW);

            // 球员总数（羽+网统一）
            LambdaQueryWrapper<Player> playerW = new LambdaQueryWrapper<>();
            playerW.eq(Player::getTenantId, tenantId);
            Long totalPlayers = playerMapper.selectCount(playerW);

            // 店铺总数
            Long totalShops = shopMapper.selectCount(
                    new LambdaQueryWrapper<Shop>().eq(Shop::getTenantId, tenantId));

            data.put("todayOrders", todayOrders);
            data.put("pendingOrders", pendingOrders);
            data.put("totalPlayers", totalPlayers);
            data.put("totalShops", totalShops);
        }

        return Result.success(data);
    }
}
