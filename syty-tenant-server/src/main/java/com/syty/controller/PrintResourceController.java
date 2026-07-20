package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.BizException;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.PrintResource;
import com.syty.service.PrintResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
/**
 * 打印资源 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/print-resource")
@RequiredArgsConstructor
public class PrintResourceController {
    private final PrintResourceService printResourceService;
    /**
     * 分页查询打印资源
     */
    @GetMapping("/page")
    public Result<Page<PrintResource>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long shopId) {
        if (shopId == null) shopId = TenantContext.getTenantId();
        if (shopId == null) throw new BizException("店铺ID不能为空");
        Page<PrintResource> page = new Page<>(current, size);
        LambdaQueryWrapper<PrintResource> qw = new LambdaQueryWrapper<>();
        qw.eq(PrintResource::getShopId, shopId)
          .orderByDesc(PrintResource::getUpdatedAt);
        if (type != null && !type.isEmpty()) {
            qw.eq(PrintResource::getResourceKey, type);
        }
        return Result.success(printResourceService.page(page, qw));
    }
    /**
     * 保存或更新资源
     */
    @PostMapping
    public Result<Void> save(@RequestBody PrintResource resource) {
        Long shopId = TenantContext.getTenantId();
        if (shopId == null) throw new BizException("店铺ID不能为空");
        resource.setShopId(shopId.toString());
        if (resource.getId() == null) {
            resource.setCreatedAt(LocalDateTime.now());
            printResourceService.save(resource);
        } else {
            resource.setUpdatedAt(LocalDateTime.now());
            printResourceService.updateById(resource);
        }
        return Result.success();
    }
    /**
     * 删除资源
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        printResourceService.removeById(id);
        return Result.success();
    }
    /**
     * 列表查询 (供前端、C# 客户端等获取)
     */
    @GetMapping("/list")
    public Result<List<PrintResource>> list(@RequestParam(required = false) String type,
                                            @RequestParam(required = false) Long shopId) {
        if (shopId == null) shopId = TenantContext.getTenantId();
        if (shopId == null) throw new BizException("店铺ID不能为空");
        LambdaQueryWrapper<PrintResource> qw = new LambdaQueryWrapper<>();
        qw.eq(PrintResource::getShopId, shopId);
        if (type != null && !type.isEmpty()) {
            qw.eq(PrintResource::getResourceKey, type);
        }
        qw.orderByDesc(PrintResource::getUpdatedAt);
        return Result.success(printResourceService.list(qw));
    }
}
