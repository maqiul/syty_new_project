package com.syty.controller;

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

/**
 * 网球球线管理（兼容层）
 * V1.1 羽网合并后，网球球线已统一到 string_info 表（sportType='TENNIS'）
 *
 * @deprecated 后续版本将删除，前端应迁移到 /api/string?sportType=TENNIS
 */
@Deprecated
@Tag(name = "网球球线管理（兼容层）")
@RestController
@RequestMapping("/api/tennis/string")
@RequiredArgsConstructor
public class TennisStringInfoController {

    private final StringInfoService stringInfoService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<Page<StringInfo>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<StringInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringInfo::getSportType, "TENNIS");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(StringInfo::getBrand, keyword).or().like(StringInfo::getModel, keyword));
        }
        wrapper.orderByDesc(StringInfo::getId);
        return Result.success(stringInfoService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/list")
    public Result<Object> list() {
        LambdaQueryWrapper<StringInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringInfo::getSportType, "TENNIS");
        return Result.success(stringInfoService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<StringInfo> get(@PathVariable Long id) {
        return Result.success(stringInfoService.getById(id));
    }

    @PostMapping
    public Result<StringInfo> add(@RequestBody StringInfo s) {
        s.setSportType("TENNIS");
        stringInfoService.save(s);
        return Result.success(s);
    }

    @PutMapping
    public Result<Void> update(@RequestBody StringInfo s) {
        stringInfoService.updateById(s);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        stringInfoService.removeById(id);
        return Result.success();
    }
}
