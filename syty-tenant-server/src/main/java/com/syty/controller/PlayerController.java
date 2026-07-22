package com.syty.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.Player;
import com.syty.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
@Tag(name = "球员管理")
@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerService playerService;
    @Operation(summary = "分页查询球员")
    @GetMapping("/page")
    public Result<Page<Player>> page(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String sportType) {
        LambdaQueryWrapper<Player> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sportType)) {
            wrapper.eq(Player::getSportType, sportType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Player::getName, keyword).or().like(Player::getPhone, keyword));
        }
        wrapper.orderByDesc(Player::getId);
        return Result.success(playerService.page(new Page<>(page, size), wrapper));
    }
    @Operation(summary = "球员列表(下拉)")
    @GetMapping("/list")
    public Result<Object> list(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String sportType) {
        LambdaQueryWrapper<Player> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sportType)) {
            wrapper.eq(Player::getSportType, sportType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Player::getName, keyword);
        }
        return Result.success(playerService.list(wrapper));
    }
    @GetMapping("/phone/{phone}")
    public Result<Player> getByPhone(@PathVariable String phone) {
        return Result.success(playerService.lambdaQuery().eq(Player::getPhone, phone).one());
    }
    @Operation(summary = "获取球员详情")
    @GetMapping("/{id}")
    public Result<Player> get(@PathVariable Long id) {
        return Result.success(playerService.getById(id));
    }
    @SaCheckPermission("player:create")
    @Operation(summary = "新增球员")
    @PostMapping
    public Result<Player> add(@RequestBody Player player) {
        playerService.save(player);
        return Result.success(player);
    }
    @SaCheckPermission("player:edit")
    @Operation(summary = "修改球员")
    @PutMapping
    public Result<Void> update(@RequestBody Player player) {
        playerService.updateById(player);
        return Result.success();
    }
    @SaCheckPermission("player:delete")
    @Operation(summary = "删除球员")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        playerService.removeById(id);
        return Result.success();
    }
}
