package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.TennisRacket;
import com.syty.service.TennisRacketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
@Tag(name = "网球球拍管理")
@RestController
@RequestMapping("/api/tennis/racket")
@RequiredArgsConstructor
public class TennisRacketController {
    private final TennisRacketService racketService;
    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<Page<TennisRacket>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<TennisRacket> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword))
            wrapper.like(TennisRacket::getBrand, keyword).or().like(TennisRacket::getModel, keyword);
        wrapper.orderByDesc(TennisRacket::getId);
        return Result.success(racketService.page(new Page<>(page, size), wrapper));
    }
    @GetMapping("/list") public Result<Object> list() { return Result.success(racketService.list()); }
    @GetMapping("/{id}") public Result<TennisRacket> get(@PathVariable Long id) { return Result.success(racketService.getById(id)); }
    @PostMapping public Result<TennisRacket> add(@RequestBody TennisRacket r) { racketService.save(r); return Result.success(r); }
    @PutMapping public Result<Void> update(@RequestBody TennisRacket r) { racketService.updateById(r); return Result.success(); }
    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) { racketService.removeById(id); return Result.success(); }
}
