package com.syty.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.dto.InboundRequest;
import com.syty.dto.StringerVO;
import com.syty.entity.InboundOrder;
import com.syty.service.InboundService;
import com.syty.service.StringerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 入库管理 Controller
 * <p>
 * V1.4 新增:
 * - POST /api/inbound    创建入库单 (直接增加库存, 无需审核)
 * - GET  /api/inbound/{id} 查询入库单详情
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "入库管理", description = "线材入库, 直接增加门店库存")
@RequestMapping("/api/inbound")
public class InboundController {

    private final InboundService inboundService;
    private final StringerService stringerService;

    /**
     * 分页查询入库单列表
     */
    @GetMapping
    @SaCheckPermission("inbound:query")
    @Operation(summary = "查询入库单列表", description = "获取当前租户下的入库单记录")
    public Result<List<InboundOrder>> listInbound() {
        try {
            List<InboundOrder> orders = inboundService.listInbound();
            return Result.success(orders);
        } catch (Exception e) {
            log.error("查询入库单列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 创建入库单
     * 逻辑: 写入入库单 -> 直接增加 shop_string 库存 (无需审核)
     */
    @PostMapping
    @SaCheckPermission("inbound:create")
    @Operation(summary = "创建入库单", description = "接收入库数据并直接增加门店线材库存")
    public Result<InboundOrder> createInbound(@Valid @RequestBody InboundRequest request) {
        try {
            InboundOrder order = inboundService.createInbound(request);
            return Result.success(order);
        } catch (Exception e) {
            log.error("创建入库单失败: shopId={}", request.getShopId(), e);
            return Result.error("入库失败: " + e.getMessage());
        }
    }

    /**
     * 查询入库单详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("inbound:query")
    @Operation(summary = "查询入库单详情", description = "根据ID查询入库单")
    public Result<InboundOrder> getInbound(@PathVariable Long id) {
        try {
            InboundOrder order = inboundService.getOrderById(id);
            if (order == null) {
                return Result.error(404, "入库单不存在");
            }
            return Result.success(order);
        } catch (Exception e) {
            log.error("查询入库单失败: id={}", id, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取穿线师列表
     */
    @GetMapping("/stringers")
    @SaCheckPermission("inbound:query")
    @Operation(summary = "获取穿线师列表", description = "查询所有穿线师角色的用户")
    public Result<List<StringerVO>> getStringers() {
        try {
            List<StringerVO> stringers = stringerService.getStringers();
            return Result.success(stringers);
        } catch (Exception e) {
            log.error("查询穿线师列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 删除入库单 (撤销入库)
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("inbound:delete")
    @Operation(summary = "撤销入库单", description = "删除入库单并回滚库存")
    public Result<String> deleteInbound(@PathVariable Long id) {
        try {
            inboundService.deleteInbound(id);
            return Result.success("撤销成功");
        } catch (Exception e) {
            log.error("撤销入库失败: id={}", id, e);
            return Result.error("撤销失败: " + e.getMessage());
        }
    }
}
