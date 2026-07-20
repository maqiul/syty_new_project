package com.syty.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.PlatformConfig;
import com.syty.entity.PlatformNotice;
import com.syty.mapper.PlatformConfigMapper;
import com.syty.mapper.PlatformNoticeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlatformConfigService extends ServiceImpl<PlatformConfigMapper, PlatformConfig> {
    public PlatformConfig getConfig() {
        return getById(1L);
    }

    @Transactional
    public void updateConfig(PlatformConfig config) {
        config.setId(1L);
        updateById(config);
        baseMapper.incrementVersion(1L);
    }
}

@Service
class PlatformNoticeService extends ServiceImpl<PlatformNoticeMapper, PlatformNotice> {
    public List<PlatformNotice> getVisibleNotices(String tenantId) {
        return baseMapper.selectVisibleNotices(tenantId);
    }

    @Transactional
    public void publish(Long id) {
        PlatformNotice notice = new PlatformNotice();
        notice.setId(id);
        notice.setStatus("PUBLISHED");
        updateById(notice);
    }
}
