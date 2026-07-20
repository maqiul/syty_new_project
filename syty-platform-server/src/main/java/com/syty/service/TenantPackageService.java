package com.syty.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.common.BizException;
import com.syty.entity.PackageInfo;
import com.syty.entity.TenantInfo;
import com.syty.entity.TenantPackageLog;
import com.syty.mapper.PackageInfoMapper;
import com.syty.mapper.TenantInfoMapper;
import com.syty.mapper.TenantPackageLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPackageService extends ServiceImpl<TenantPackageLogMapper, TenantPackageLog> {

    private final TenantInfoMapper tenantInfoMapper;
    private final PackageInfoMapper packageInfoMapper;

    /**
     * 核心续费/调整逻辑
     */
    @Transactional(rollbackFor = Exception.class)
    public void renewPackage(Long tenantId, Long packageId, Integer durationDays, String operateType, String remark) {
        // 1. 获取当前租户信息
        TenantInfo tenant = tenantInfoMapper.selectById(tenantId);
        if (tenant == null) throw new BizException("租户不存在");

        // 2. 获取套餐信息
        PackageInfo pkg = packageInfoMapper.selectById(packageId);
        if (pkg == null) throw new BizException("套餐不存在");

        // 3. 确定天数 (前端传 null 则使用套餐默认天数)
        int days = (durationDays == null || durationDays == 0) ? pkg.getDurationDays() : durationDays;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldExpiredAt = tenant.getPackageExpiredAt();
        LocalDateTime newExpiredAt;
        int gapDays = 0;

        // 4. 计算新到期时间
        if (oldExpiredAt != null && oldExpiredAt.isAfter(now)) {
            // 未过期：叠加
            newExpiredAt = oldExpiredAt.plusDays(days);
        } else {
            // 已过期或无记录：从当前时间算
            newExpiredAt = now.plusDays(days);
            if (oldExpiredAt != null) {
                gapDays = (int) Duration.between(oldExpiredAt, now).toDays();
            }
        }

        // 5. 缩短权益校验与计算
        int shrinkDays = 0;
        if (oldExpiredAt != null && newExpiredAt.isBefore(oldExpiredAt)) {
            shrinkDays = (int) Duration.between(newExpiredAt, oldExpiredAt).toDays();
            // 强校验：缩短必须填备注
            if (remark == null || remark.trim().isEmpty()) {
                throw new BizException("缩短租户权益必须填写备注原因！");
            }
            log.warn("⚠️ 检测到缩短权益操作: tenantId={}, shrink={} days", tenantId, shrinkDays);
        }

        // 6. 更新租户表
        tenant.setPackageId(packageId);
        tenant.setPackageExpiredAt(newExpiredAt);
        tenant.setPackageStatus("ACTIVE");
        tenantInfoMapper.updateById(tenant);

        // 7. 写入全快照日志
        TenantPackageLog logEntity = new TenantPackageLog();
        logEntity.setTenantId(tenantId);
        logEntity.setPackageId(packageId);
        logEntity.setOperateType(operateType != null ? operateType : "RENEW");
        logEntity.setDurationDays(days);
        logEntity.setGapDays(gapDays);
        logEntity.setShrinkDays(shrinkDays > 0 ? shrinkDays : 0);
        logEntity.setRemark(remark);
        logEntity.setBeforeExpiredAt(oldExpiredAt);
        logEntity.setAfterExpiredAt(newExpiredAt);
        logEntity.setCreatedAt(now);
        
        // 获取操作人 (如果未登录则默认为 0)
        try {
            logEntity.setOperatorId(StpUtil.getLoginIdAsLong());
        } catch (Exception e) {
            logEntity.setOperatorId(0L);
        }

        save(logEntity);
        log.info("✅ 租户续费成功: tenantId={}, days={}, type={}", tenantId, days, operateType);
    }
}
