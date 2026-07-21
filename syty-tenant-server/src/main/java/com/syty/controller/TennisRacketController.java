package com.syty.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.Racket;
import com.syty.service.RacketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 网球球拍管理（兼容层）
 * V1.1 羽网合并后，网球球拍已统一到 racket 表（sportType='TENNIS'）
 *
 * @deprecated 后续版本将删除，前端应迁移到 /api/racket?sportType=TENNIS
 */
@Deprecated
@Tag(name = "网球球拍管理（兼容层）")
@RestController
@RequestMapping("/api/tennis/racket")
@RequiredArgsConstructor
public class TennisRacketController {

    private final RacketService racketService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<Page<Racket>> page(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Racket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Racket::getSportType, "TENNIS");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Racket::getBrand, keyword).or().like(Racket::getModel, keyword));
        }
        wrapper.orderByDesc(Racket::getId);
        return Result.success(racketService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/list")
    public Result<Object> list() {
        LambdaQueryWrapper<Racket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Racket::getSportType, "TENNIS");
        return Result.success(racketService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<Racket> get(@PathVariable Long id) {
        return Result.success(racketService.getById(id));
    }

    @PostMapping
    public Result<Racket> add(@RequestBody Racket r) {
        r.setSportType("TENNIS");
        racketService.save(r);
        return Result.success(r);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Racket r) {
        racketService.updateById(r);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        racketService.removeById(id);
        return Result.success();
    }
}
