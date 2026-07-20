package com.syty.controller;

import com.syty.common.Result;
import com.syty.dto.PerformanceStatsDTO;
import com.syty.dto.StringerRankDTO;
import com.syty.service.PerformanceService;
import com.syty.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    // 租户端统计
    @GetMapping("/api/v1/tenant/performance/stats")
    public Result<PerformanceStatsDTO> getTenantStats(@RequestParam(defaultValue = "month") String period) {
        Long tenantId = TenantContext.getTenantId() != null ? Long.valueOf(TenantContext.getTenantId()) : 1L;
        return Result.success(performanceService.getTenantStats(tenantId, period));
    }

    // 租户端排行榜
    @GetMapping("/api/v1/tenant/performance/stringer-rank")
    public Result<List<StringerRankDTO>> getStringerRank(@RequestParam(defaultValue = "month") String period,
                                                          @RequestParam(defaultValue = "5") int limit) {
        Long tenantId = TenantContext.getTenantId() != null ? Long.valueOf(TenantContext.getTenantId()) : 1L;
        return Result.success(performanceService.getStringerRank(tenantId, period, limit));
    }

    // 管理端全局统计
    @GetMapping("/api/v1/admin/performance/stats")
    public Result<PerformanceStatsDTO> getGlobalStats(@RequestParam(required = false) Long tenantIdFilter,
                                                       @RequestParam(defaultValue = "month") String period) {
        return Result.success(performanceService.getGlobalStats(tenantIdFilter, period));
    }
}
