package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.TennisStringInfo;
import com.syty.service.TennisStringInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
@Tag(name = "网球球线管理")
@RestController
@RequestMapping("/api/tennis/string")
@RequiredArgsConstructor
public class TennisStringInfoController {
    private final TennisStringInfoService stringService;
    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<Page<TennisStringInfo>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<TennisStringInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword))
            wrapper.like(TennisStringInfo::getBrand, keyword).or().like(TennisStringInfo::getModel, keyword);
        wrapper.orderByDesc(TennisStringInfo::getId);
        return Result.success(stringService.page(new Page<>(page, size), wrapper));
    }
    @GetMapping("/list") public Result<Object> list() { return Result.success(stringService.list()); }
    @GetMapping("/{id}") public Result<TennisStringInfo> get(@PathVariable Long id) { return Result.success(stringService.getById(id)); }
    @PostMapping public Result<TennisStringInfo> add(@RequestBody TennisStringInfo s) { stringService.save(s); return Result.success(s); }
    @PutMapping public Result<Void> update(@RequestBody TennisStringInfo s) { stringService.updateById(s); return Result.success(); }
    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) { stringService.removeById(id); return Result.success(); }
}
