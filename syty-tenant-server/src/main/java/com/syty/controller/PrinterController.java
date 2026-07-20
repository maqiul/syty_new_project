package com.syty.controller;

import com.syty.common.Result;
import com.syty.dto.PrintSetupResponse;
import com.syty.dto.PrintTaskReportRequest;
import com.syty.dto.PrinterRegisterRequest;
import com.syty.dto.StockReservationResult;
import com.syty.service.InventoryService;
import com.syty.service.PrinterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 打印配置管理 Controller
 *
 * V1.2 新增:
 * - POST /api/print-task/report        打印回执上报 (C#客户端)
 * - POST /api/order/generate-id        生成订单号 (雪花算法)
 * - POST /api/inventory/reserve        库存预留
 * - POST /api/inventory/deduct         库存实扣
 * - POST /api/inventory/release        释放预留
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "打印配置管理", description = "打印机注册、配置拉取、资源上传、打印回执")
public class PrinterController {

    private final PrinterService printerService;
    private final InventoryService inventoryService;

    // ==================== 兼容旧版 GET 注册接口 ====================
    @GetMapping("/api/printer/register")
    @Operation(summary = "打印机注册 (GET兼容)")
    public Result<Void> registerPrinterGet(
            @RequestParam String shopId,
            @RequestParam(required = false) String tenantId) {
        log.info("收到 GET 注册请求: shopId={}, tenantId={}", shopId, tenantId);
        // 兼容旧版请求，仅记录日志，避免 404
        return Result.success();
    }

    // ==================== API 1: 注册打印机 ====================
    @PostMapping("/api/v1/printer/register")
    @Operation(summary = "注册打印机", description = "幂等注册: 同一 shopId+machineId 会覆盖旧的打印机列表")
    public Result<Void> registerPrinter(@Valid @RequestBody PrinterRegisterRequest request) {
        try {
            printerService.registerPrinter(request);
            return Result.success();
        } catch (Exception e) {
            log.error("注册打印机失败", e);
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    // ==================== API 2: 拉取配置 (JOIN 优化版) ====================
    @GetMapping("/api/v1/print-setup")
    @Operation(summary = "拉取打印配置", description = "获取指定机器的打印机列表、打印规则、静态资源 (V1.2 JOIN优化)")
    public Result<PrintSetupResponse> getPrintSetup(
            @Parameter(description = "门店ID", required = true)
            @RequestParam String shopId,
            @Parameter(description = "机器唯一标识 (Windows GUID)", required = true)
            @RequestParam String machineId) {
        try {
            PrintSetupResponse response = printerService.getPrintSetup(shopId, machineId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("拉取打印配置失败: shopId={}, machineId={}", shopId, machineId, e);
            return Result.error("拉取配置失败: " + e.getMessage());
        }
    }

    // ==================== OSS 图片上传 ====================
    @PostMapping("/api/v1/resource/upload")
    @Operation(summary = "上传打印资源图片", description = "上传 Logo、收款码等图片到 MinIO/OSS")
    public Result<String> uploadResource(
            @Parameter(description = "门店ID (空字符串表示全局资源)")
            @RequestParam(required = false, defaultValue = "") String shopId,
            @Parameter(description = "资源标识 (如 LOGO, QR_CODE_PAY, QR_CODE_MINI)", required = true)
            @RequestParam String resourceKey,
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            String url = printerService.uploadResource(shopId, resourceKey, file);
            return Result.success(url);
        } catch (Exception e) {
            log.error("上传资源失败: shopId={}, resourceKey={}", shopId, resourceKey, e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    // ==================== V1.2 新增: 打印回执上报 ====================
    @PostMapping("/api/print-task/report")
    @Operation(summary = "打印回执上报", description = "C# 客户端打印完成后上报结果, 保存到 print_task_log 表")
    public Result<Void> reportPrintTask(@Valid @RequestBody PrintTaskReportRequest request) {
        try {
            printerService.reportPrintTask(request);
            return Result.success();
        } catch (Exception e) {
            log.error("打印回执上报失败: orderId={}", request.getOrderId(), e);
            return Result.error("上报失败: " + e.getMessage());
        }
    }

    // ==================== V1.2 新增: 生成订单号 (雪花算法) ====================
    @PostMapping("/api/order/generate-id")
    @Operation(summary = "生成订单号", description = "使用雪花算法生成全局唯一订单号, 防止撞单")
    public Result<String> generateOrderId() {
        try {
            String orderId = printerService.generateOrderId();
            return Result.success(orderId);
        } catch (Exception e) {
            log.error("生成订单号失败", e);
            return Result.error("生成订单号失败: " + e.getMessage());
        }
    }

    // ==================== V1.2 新增: 库存预留 ====================
    @PostMapping("/api/inventory/reserve")
    @Operation(summary = "库存预留 (接单预扣)", description = "接单时预扣减 reserved_stock")
    public Result<StockReservationResult> reserveStock(
            @Parameter(description = "商品编号", required = true)
            @RequestParam String skuCode,
            @Parameter(description = "数量", required = true)
            @RequestParam int amount) {
        try {
            StockReservationResult result = printerService.reserveStockForOrder(skuCode, amount);
            if (result.isSuccess()) {
                return Result.success(result);
            } else {
                return Result.error(400, result.getMessage());
            }
        } catch (Exception e) {
            log.error("库存预留失败: skuCode={}", skuCode, e);
            return Result.error("库存预留失败: " + e.getMessage());
        }
    }

    @PostMapping("/api/inventory/deduct")
    @Operation(summary = "库存实扣 (完成扣减)", description = "订单完成时从 totalStock 实扣, 同时释放 reserved_stock")
    public Result<Void> deductStock(
            @Parameter(description = "商品编号", required = true)
            @RequestParam String skuCode,
            @Parameter(description = "数量", required = true)
            @RequestParam int amount) {
        try {
            printerService.deductStockOnComplete(skuCode, amount);
            return Result.success();
        } catch (Exception e) {
            log.error("库存实扣失败: skuCode={}", skuCode, e);
            return Result.error("库存实扣失败: " + e.getMessage());
        }
    }

    @PostMapping("/api/inventory/release")
    @Operation(summary = "释放预留库存", description = "订单取消时释放 reserved_stock")
    public Result<Void> releaseStock(
            @Parameter(description = "商品编号", required = true)
            @RequestParam String skuCode,
            @Parameter(description = "数量", required = true)
            @RequestParam int amount) {
        try {
            inventoryService.releaseReservedStock(skuCode, amount);
            return Result.success();
        } catch (Exception e) {
            log.error("释放预留库存失败: skuCode={}", skuCode, e);
            return Result.error("释放预留库存失败: " + e.getMessage());
        }
    }
}
