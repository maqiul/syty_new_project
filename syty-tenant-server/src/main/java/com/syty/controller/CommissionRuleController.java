package com.syty.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.common.Result;
import com.syty.entity.CommissionRule;
import com.syty.mapper.CommissionRuleMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "提成规则管理")
@RestController
@RequestMapping("/api/commission/rule")
@RequiredArgsConstructor
public class CommissionRuleController {
    private final CommissionRuleMapper commissionRuleMapper;

    @Operation(summary = "查询提成规则列表")
    @GetMapping("/list")
    public Result<List<CommissionRule>> list() {
        LambdaQueryWrapper<CommissionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CommissionRule::getCreatedAt);
        return Result.success(commissionRuleMapper.selectList(wrapper));
    }

    @SaCheckPermission("commission:create")
    @Operation(summary = "新增提成规则")
    @PostMapping
    public Result<CommissionRule> add(@RequestBody CommissionRule rule) {
        commissionRuleMapper.insert(rule);
        return Result.success(rule);
    }

    @SaCheckPermission("commission:edit")
    @Operation(summary = "修改提成规则")
    @PutMapping
    public Result<Void> update(@RequestBody CommissionRule rule) {
        commissionRuleMapper.updateById(rule);
        return Result.success();
    }

    @SaCheckPermission("commission:delete")
    @Operation(summary = "删除提成规则")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commissionRuleMapper.deleteById(id);
        return Result.success();
    }
}
