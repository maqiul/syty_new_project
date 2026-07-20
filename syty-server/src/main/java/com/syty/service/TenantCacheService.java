package com.syty.service;
import com.syty.entity.Tenant;
import com.syty.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
/**
 * 租户大赛开关缓存（Redis字
 */
@Service
@RequiredArgsConstructor
public class TenantCacheService {
    private final TenantMapper tenantMapper;
    /**
     * 获取租户大赛开字
     * 缓存 key = "tenant:tournament::{tenantId}"
     */
    @Cacheable(value = "tenant:tournament", key = "#tenantId", unless = "#result == null")
    public TournamentFlags getTournamentFlags(Long tenantId) {
        if (tenantId == null) return null;
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) return null;
        return new TournamentFlags(
            tenant.getEnableBadmintonTournament() == null || tenant.getEnableBadmintonTournament() == 1,
            tenant.getEnableTennisTournament() != null && tenant.getEnableTennisTournament() == 1
        );
    }
    /** 清除指定租户的开关缓存（租户更新时调用） */
    @CacheEvict(value = "tenant:tournament", key = "#tenantId")
    public void evictTournamentFlags(Long tenantId) {
        // 仅清除缓字
    }
    /** immutable 数据传输对象 */
    public record TournamentFlags(boolean badminton, boolean tennis) {}
}
