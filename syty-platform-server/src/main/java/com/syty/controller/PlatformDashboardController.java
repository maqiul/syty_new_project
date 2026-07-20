package com.syty.controller;

import com.syty.common.Result;
import com.syty.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 平台端 Dashboard 统计接口
 */
@Tag(name = "平台Dashboard")
@Slf4j
@RestController
@RequestMapping("/api/platform/dashboard")
@RequiredArgsConstructor
public class PlatformDashboardController {

    private final TenantMapper tenantMapper;
    private final SysUserMapper sysUserMapper;
    private final PackageInfoMapper packageMapper;
    private final OperateLogMapper operateLogMapper;

    @Operation(summary = "获取平台统计概览")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        try {
            // 租户总数
            Long tenantCount = tenantMapper.selectCount(null);
            stats.put("tenantCount", tenantCount != null ? tenantCount : 0);
            
            // 活跃租户数 (status=1)
            Long activeTenantCount = tenantMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.syty.entity.Tenant>()
                    .eq(com.syty.entity.Tenant::getStatus, 1)
            );
            stats.put("activeTenantCount", activeTenantCount != null ? activeTenantCount : 0);
            
            // 用户总数
            Long userCount = sysUserMapper.selectCount(null);
            stats.put("userCount", userCount != null ? userCount : 0);
            
            // 套餐列表
            List<Map<String, Object>> packageDistribution = new ArrayList<>();
            var packages = packageMapper.selectList(null);
            for (var pkg : packages) {
                Map<String, Object> item = new HashMap<>();
                item.put("packageName", pkg.getName());
                item.put("status", pkg.getStatus());
                item.put("price", pkg.getPrice());
                packageDistribution.add(item);
            }
            stats.put("packageDistribution", packageDistribution);
            
        } catch (Exception e) {
            log.error("获取平台统计失败", e);
            stats.put("tenantCount", 0);
            stats.put("activeTenantCount", 0);
            stats.put("userCount", 0);
            stats.put("packageDistribution", new ArrayList<>());
        }
        
        return Result.success(stats);
    }

    @Operation(summary = "获取最近操作日志")
    @GetMapping("/logs")
    public Result<List<Map<String, Object>>> getRecentLogs(@RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> logs = new ArrayList<>();
        try {
            var logList = operateLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.syty.entity.OperateLog>()
                    .orderByDesc(com.syty.entity.OperateLog::getCreatedAt)
                    .last("LIMIT " + limit)
            );
            for (var log : logList) {
                Map<String, Object> item = new HashMap<>();
                item.put("user", log.getUsername() != null ? log.getUsername() : "系统");
                item.put("content", log.getDetail() != null ? log.getDetail() : log.getOperation());
                item.put("time", log.getCreatedAt() != null ? log.getCreatedAt().toString() : "");
                item.put("avatarColor", "#1677ff");
                logs.add(item);
            }
        } catch (Exception e) {
            log.error("获取操作日志失败", e);
        }
        return Result.success(logs);
    }
}
