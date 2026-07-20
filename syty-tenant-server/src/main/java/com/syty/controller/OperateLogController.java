package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.OperateLog;
import com.syty.mapper.OperateLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/operate-log")
@RequiredArgsConstructor
public class OperateLogController {
    private final OperateLogMapper operateLogMapper;
    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    public Result<Page<OperateLog>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) String module,
                                          @RequestParam(required = false) String action) {
        LambdaQueryWrapper<OperateLog> wrapper = new LambdaQueryWrapper<>();
        // 租户隔离
        if (!TenantContext.isSuperAdmin()) {
            wrapper.eq(OperateLog::getTenantId, TenantContext.getTenantId());
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperateLog::getModule, module);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(OperateLog::getOperation, action);
        }
        wrapper.orderByDesc(OperateLog::getId);
        return Result.success(operateLogMapper.selectPage(new Page<>(page, size), wrapper));
    }
}
