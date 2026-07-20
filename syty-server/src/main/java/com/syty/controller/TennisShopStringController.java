package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.TennisShopString;
import com.syty.service.TennisShopStringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name = "网球店铺球线库存")
@RestController
@RequestMapping("/api/tennis/shop-string")
@RequiredArgsConstructor
public class TennisShopStringController {
    private final TennisShopStringService shopStringService;
    @GetMapping("/shop/{shopId}")
    @Operation(summary = "查询店铺球线库存")
    public Result<Object> list(@PathVariable Long shopId) {
        return Result.success(shopStringService.lambdaQuery().eq(TennisShopString::getShopId, shopId).list());
    }
    @PostMapping
    @Operation(summary = "添加")
    public Result<Void> add(@RequestBody TennisShopString s) {
        s.setTenantId(TenantContext.getTenantId());
        shopStringService.save(s);
        return Result.success();
    }
    @PutMapping
    @Operation(summary = "修改")
    public Result<Void> update(@RequestBody TennisShopString s) {
        shopStringService.updateById(s);
        return Result.success();
    }
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { shopStringService.removeById(id); return Result.success(); }
}
