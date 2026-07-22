package com.syty.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.BizException;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.PrintRule;
import com.syty.service.PrintRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/print-rule")
@RequiredArgsConstructor
public class PrintRuleController {
    private final PrintRuleService printRuleService;
    /** 分页查询 */
    @GetMapping("/page")
    public Result<Page<PrintRule>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Long tenantId) {
        // 🆕 优先使用前端传来的shopId，如果没有则尝试从Token 获取
        if (shopId == null) shopId = TenantContext.getTenantId();
        if (shopId == null) throw new BizException("店铺ID不能为空");
        LambdaQueryWrapper<PrintRule> qw = new LambdaQueryWrapper<>();
        qw.eq(PrintRule::getShopId, shopId);
        if (docType != null && !docType.isEmpty()) {
            qw.eq(PrintRule::getDocType, docType);
        }
        qw.orderByAsc(PrintRule::getSortOrder);
        Page<PrintRule> page = printRuleService.page(new Page<>(current, size), qw);
        return Result.success(page);
    }
    /** 列表查询 */
    @GetMapping("/list")
    public Result<List<PrintRule>> list(
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) Long shopId) {
        if (shopId == null) shopId = TenantContext.getTenantId();
        if (shopId == null) throw new BizException("店铺ID不能为空");
        LambdaQueryWrapper<PrintRule> qw = new LambdaQueryWrapper<>();
        qw.eq(PrintRule::getShopId, shopId);
        if (docType != null && !docType.isEmpty()) {
            qw.eq(PrintRule::getDocType, docType);
        }
        qw.orderByAsc(PrintRule::getSortOrder);
        return Result.success(printRuleService.list(qw));
    }
    /** 新增或修改*/
    @SaCheckPermission("print:rule:edit")
    @PostMapping
    public Result<Void> save(@RequestBody PrintRule rule,
                             @RequestParam(required = false) Long shopId) {
        if (shopId == null) shopId = TenantContext.getTenantId();
        if (shopId == null) throw new BizException("店铺ID不能为空");
        rule.setShopId(shopId.toString());
        printRuleService.saveOrUpdate(rule);
        return Result.success();
    }
    /** 删除 */
    @SaCheckPermission("print:rule:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        printRuleService.removeById(id);
        return Result.success();
    }
}
