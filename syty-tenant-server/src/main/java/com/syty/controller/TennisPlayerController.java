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

/**
 * 网球球员管理（兼容层）
 * V1.1 羽网合并后，网球球员已统一到 player 表（sportType='TENNIS'）
 * 此 Controller 保留旧 API 路径以兼容前端，内部代理到统一 PlayerService
 *
 * @deprecated 后续版本将删除，前端应迁移到 /api/player?sportType=TENNIS
 */
@Deprecated
@Tag(name = "网球球员管理（兼容层）")
@RestController
@RequestMapping("/api/tennis/player")
@RequiredArgsConstructor
public class TennisPlayerController {

    private final PlayerService playerService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result<Page<Player>> page(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Player> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Player::getSportType, "TENNIS");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Player::getName, keyword).or().like(Player::getPhone, keyword));
        }
        wrapper.orderByDesc(Player::getId);
        return Result.success(playerService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/list")
    @Operation(summary = "列表")
    public Result<Object> list() {
        LambdaQueryWrapper<Player> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Player::getSportType, "TENNIS");
        return Result.success(playerService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<Player> get(@PathVariable Long id) {
        return Result.success(playerService.getById(id));
    }

    @SaCheckPermission("player:create")
    @PostMapping
    public Result<Player> add(@RequestBody Player p) {
        p.setSportType("TENNIS");
        playerService.save(p);
        return Result.success(p);
    }

    @SaCheckPermission("player:edit")
    @PutMapping
    public Result<Void> update(@RequestBody Player p) {
        playerService.updateById(p);
        return Result.success();
    }

    @SaCheckPermission("player:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        playerService.removeById(id);
        return Result.success();
    }
}
