package com.syty.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.dto.PerformanceStatsDTO;
import com.syty.dto.StringerRankDTO;
import com.syty.entity.StringerWorkLog;
import com.syty.mapper.StringerWorkLogMapper;
import com.syty.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private final StringerWorkLogMapper workLogMapper;

    @Override
    public PerformanceStatsDTO getTenantStats(Long tenantId, String period) {
        LambdaQueryWrapper<StringerWorkLog> qw = new LambdaQueryWrapper<>();
        qw.eq(StringerWorkLog::getTenantId, tenantId);
        if ("month".equals(period)) {
            LocalDateTime start = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            qw.ge(StringerWorkLog::getWorkTime, start);
        }
        
        List<StringerWorkLog> logs = workLogMapper.selectList(qw);
        PerformanceStatsDTO stats = new PerformanceStatsDTO();
        stats.setTotalOrders((long) logs.size());
        stats.setTotalFee(logs.stream().map(StringerWorkLog::getFee)
                .filter(f -> f != null).reduce(BigDecimal.ZERO, BigDecimal::add));
        stats.setAvgDuration(logs.stream().mapToInt(l -> l.getDuration() != null ? l.getDuration() : 0).average().orElse(0.0));
        return stats;
    }

    @Override
    public List<StringerRankDTO> getStringerRank(Long tenantId, String period, int limit) {
        // 简化：直接查全量，实际可按 period 过滤
        List<StringerRankDTO> ranks = workLogMapper.selectRankByTenant(tenantId, limit);
        // 填充 rank
        for (int i = 0; i < ranks.size(); i++) {
            ranks.get(i).setRank(i + 1);
        }
        return ranks;
    }

    @Override
    public PerformanceStatsDTO getGlobalStats(Long tenantIdFilter, String period) {
        LambdaQueryWrapper<StringerWorkLog> qw = new LambdaQueryWrapper<>();
        if (tenantIdFilter != null) qw.eq(StringerWorkLog::getTenantId, tenantIdFilter);
        
        List<StringerWorkLog> logs = workLogMapper.selectList(qw);
        PerformanceStatsDTO stats = new PerformanceStatsDTO();
        stats.setTotalOrders((long) logs.size());
        stats.setTotalFee(logs.stream().map(StringerWorkLog::getFee)
                .filter(f -> f != null).reduce(BigDecimal.ZERO, BigDecimal::add));
        stats.setAvgDuration(logs.stream().mapToInt(l -> l.getDuration() != null ? l.getDuration() : 0).average().orElse(0.0));
        return stats;
    }
}
