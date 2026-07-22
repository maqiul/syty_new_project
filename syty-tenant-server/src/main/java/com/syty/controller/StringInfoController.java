package com.syty.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.StringInfo;
import com.syty.service.StringInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
@Tag(name = "球线管理")
@RestController
@RequestMapping("/api/string")
@RequiredArgsConstructor
public class StringInfoController {
    private final StringInfoService stringInfoService;
    @Operation(summary = "分页查询球线")
    @GetMapping("/page")
    public Result<Page<StringInfo>> page(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String sportType) {
        LambdaQueryWrapper<StringInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sportType)) {
            wrapper.eq(StringInfo::getSportType, sportType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(StringInfo::getBrand, keyword).or().like(StringInfo::getModel, keyword));
        }
        wrapper.orderByDesc(StringInfo::getId);
        return Result.success(stringInfoService.page(new Page<>(page, size), wrapper));
    }
    @Operation(summary = "球线列表(下拉)")
    @GetMapping("/list")
    public Result<Object> list(@RequestParam(required = false) String sportType) {
        LambdaQueryWrapper<StringInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sportType)) {
            wrapper.eq(StringInfo::getSportType, sportType);
        }
        return Result.success(stringInfoService.list(wrapper));
    }
    @Operation(summary = "获取球线详情")
    @GetMapping("/{id}")
    public Result<StringInfo> get(@PathVariable Long id) {
        return Result.success(stringInfoService.getById(id));
    }
    @SaCheckPermission("string:create")
    @Operation(summary = "新增球线")
    @PostMapping
    public Result<StringInfo> add(@RequestBody StringInfo stringInfo) {
        stringInfoService.save(stringInfo);
        return Result.success(stringInfo);
    }
    @SaCheckPermission("string:edit")
    @Operation(summary = "修改球线")
    @PutMapping
    public Result<Void> update(@RequestBody StringInfo stringInfo) {
        stringInfoService.updateById(stringInfo);
        return Result.success();
    }
    @SaCheckPermission("string:delete")
    @Operation(summary = "删除球线")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        stringInfoService.removeById(id);
        return Result.success();
    }
}
