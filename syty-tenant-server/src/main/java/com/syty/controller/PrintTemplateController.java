package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.PrintTemplate;
import com.syty.entity.StringingOrder;
import com.syty.entity.Shop;
import com.syty.common.TenantContext;
import com.syty.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/print-template")
@RequiredArgsConstructor
public class PrintTemplateController {
    private final PrintTemplateService printTemplateService;
    private final ShopService shopService;
    private final PlayerService playerService;
    private final RacketService racketService;
    private final StringInfoService stringInfoService;
    private final StringingOrderService stringingOrderService;
    @GetMapping("/page")
    public Result<Page<PrintTemplate>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<PrintTemplate> wrapper = new LambdaQueryWrapper<PrintTemplate>()
                .orderByDesc(PrintTemplate::getIsDefault).orderByDesc(PrintTemplate::getId);
        return Result.success(printTemplateService.page(new Page<>(page, size), wrapper));
    }
    @GetMapping("/list")
    public Result<List<PrintTemplate>> list(@RequestParam(required = false) Long shopId,
                                            @RequestParam(required = false) Long tenantId) {
        Long finalTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();
        LambdaQueryWrapper<PrintTemplate> wrapper = new LambdaQueryWrapper<>();
        if (finalTenantId != null) {
            wrapper.eq(PrintTemplate::getTenantId, finalTenantId);
        }
        wrapper.orderByDesc(PrintTemplate::getIsDefault).orderByDesc(PrintTemplate::getId);
        return Result.success(printTemplateService.list(wrapper));
    }
    @GetMapping("/{id}")
    public Result<PrintTemplate> get(@PathVariable Long id) {
        return Result.success(printTemplateService.getById(id));
    }
    @GetMapping("/default")
    public Result<PrintTemplate> getDefault() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return Result.error("无租户信");
        return Result.success(printTemplateService.getDefaultTemplate(tenantId));
    }
    @PostMapping
    public Result<Void> add(@RequestBody PrintTemplate template) {
        if (template.getIsDefault() == 1) {
            printTemplateService.lambdaUpdate()
                    .eq(PrintTemplate::getTenantId, TenantContext.getTenantId())
                    .set(PrintTemplate::getIsDefault, 0)
                    .update();
        }
        printTemplateService.save(template);
        return Result.success();
    }
    @PutMapping
    public Result<Void> update(@RequestBody PrintTemplate template) {
        if (template.getIsDefault() == 1) {
            printTemplateService.lambdaUpdate()
                    .eq(PrintTemplate::getTenantId, TenantContext.getTenantId())
                    .set(PrintTemplate::getIsDefault, 0)
                    .update();
        }
        printTemplateService.updateById(template);
        return Result.success();
    }
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        printTemplateService.removeById(id);
        return Result.success();
    }
    @GetMapping("/preview/{orderId}")
    public Result<Map<String, Object>> preview(@PathVariable Long orderId) {
        StringingOrder order = stringingOrderService.getById(orderId);
        if (order == null) return Result.error("订单不存");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("playerName", playerService.getById(order.getPlayerId()).getName());
        data.put("racketName", racketService.getById(order.getRacketId()).getBrand() + " " + racketService.getById(order.getRacketId()).getModel());
        data.put("mainTension", order.getMainTension());
        data.put("crossTension", order.getCrossTension());
        data.put("totalPrice", order.getTotalPrice());
        data.put("createdAt", order.getCreatedAt());
        Shop shop = shopService.getById(order.getShopId());
        if (shop != null) data.put("shopName", shop.getName());
        PrintTemplate template = printTemplateService.getDefaultTemplate(order.getTenantId());
        String content = template != null ? template.getFieldsJson() : "默认打印模板内容";
        data.put("templateContent", content);
        return Result.success(data);
    }
}
