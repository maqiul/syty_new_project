package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.TennisPlayer;
import com.syty.service.TennisPlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
@Tag(name = "网球球员管理")
@RestController
@RequestMapping("/api/tennis/player")
@RequiredArgsConstructor
public class TennisPlayerController {
    private final TennisPlayerService playerService;
    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result<Page<TennisPlayer>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<TennisPlayer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword))
            wrapper.like(TennisPlayer::getName, keyword).or().like(TennisPlayer::getPhone, keyword);
        wrapper.orderByDesc(TennisPlayer::getId);
        return Result.success(playerService.page(new Page<>(page, size), wrapper));
    }
    @GetMapping("/list")
    @Operation(summary = "列表")
    public Result<Object> list() { return Result.success(playerService.list()); }
    @GetMapping("/{id}")
    public Result<TennisPlayer> get(@PathVariable Long id) { return Result.success(playerService.getById(id)); }
    @PostMapping
    public Result<TennisPlayer> add(@RequestBody TennisPlayer p) { playerService.save(p); return Result.success(p); }
    @PutMapping
    public Result<Void> update(@RequestBody TennisPlayer p) { playerService.updateById(p); return Result.success(); }
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { playerService.removeById(id); return Result.success(); }
}
