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
@Tag(name = "球拍管理")
@RestController
@RequestMapping("/api/racket")
@RequiredArgsConstructor
public class RacketController {
    private final RacketService racketService;
    @Operation(summary = "分页查询球拍")
    @GetMapping("/page")
    public Result<Page<Racket>> page(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String sportType) {
        LambdaQueryWrapper<Racket> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sportType)) {
            wrapper.eq(Racket::getSportType, sportType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Racket::getBrand, keyword).or().like(Racket::getModel, keyword));
        }
        wrapper.orderByDesc(Racket::getId);
        return Result.success(racketService.page(new Page<>(page, size), wrapper));
    }
    @Operation(summary = "球拍列表(下拉)")
    @GetMapping("/list")
    public Result<Object> list(@RequestParam(required = false) String sportType) {
        LambdaQueryWrapper<Racket> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sportType)) {
            wrapper.eq(Racket::getSportType, sportType);
        }
        return Result.success(racketService.list(wrapper));
    }
    @Operation(summary = "获取球拍详情")
    @GetMapping("/{id}")
    public Result<Racket> get(@PathVariable Long id) {
        return Result.success(racketService.getById(id));
    }
    @Operation(summary = "新增球拍")
    @PostMapping
    public Result<Racket> add(@RequestBody Racket racket) {
        racketService.save(racket);
        return Result.success(racket);
    }
    @Operation(summary = "修改球拍")
    @PutMapping
    public Result<Void> update(@RequestBody Racket racket) {
        racketService.updateById(racket);
        return Result.success();
    }
    @Operation(summary = "删除球拍")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        racketService.removeById(id);
        return Result.success();
    }
}
