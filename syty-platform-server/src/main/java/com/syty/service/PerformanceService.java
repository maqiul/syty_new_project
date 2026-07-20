package com.syty.service;

import com.syty.dto.PerformanceStatsDTO;
import com.syty.dto.StringerRankDTO;

import java.util.List;

public interface PerformanceService {

    /** 获取绩效统计 (租户端) */
    PerformanceStatsDTO getTenantStats(Long tenantId, String period);

    /** 获取穿线师排行榜 */
    List<StringerRankDTO> getStringerRank(Long tenantId, String period, int limit);

    /** 获取全局统计 (管理端) */
    PerformanceStatsDTO getGlobalStats(Long tenantIdFilter, String period);
}
