package com.syty.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.PrintLog;
import com.syty.mapper.PrintLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@Tag(name = "打印日志")
@RestController
@RequestMapping("/api/print-log")
@RequiredArgsConstructor
public class PrintLogController {
    private final PrintLogMapper printLogMapper;

    @GetMapping("/page")
    @SaCheckPermission("print-log:page")
    @Operation(summary = "分页查询打印日志")
    public Result<Page<PrintLog>> page(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Long shopId,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) LocalDateTime startTime,
                                       @RequestParam(required = false) LocalDateTime endTime) {
        LambdaQueryWrapper<PrintLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(PrintLog::getOrderNo, keyword)
                   .or().like(PrintLog::getPrinterName, keyword);
        }
        if (shopId != null) wrapper.eq(PrintLog::getShopId, shopId);
        if (StringUtils.hasText(status)) wrapper.eq(PrintLog::getStatus, status);
        if (startTime != null) wrapper.ge(PrintLog::getPrintTime, startTime);
        if (endTime != null) wrapper.le(PrintLog::getPrintTime, endTime);
        wrapper.orderByDesc(PrintLog::getPrintTime);
        return Result.success(printLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }
}
