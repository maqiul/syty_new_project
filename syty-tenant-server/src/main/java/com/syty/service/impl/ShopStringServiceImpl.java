package com.syty.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.ShopString;
import com.syty.mapper.ShopStringMapper;
import com.syty.service.ShopStringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ShopStringServiceImpl extends ServiceImpl<ShopStringMapper, ShopString> implements ShopStringService {

    @Override
    @Transactional
    public void reserveStock(Long shopStringId, int quantity) {
        ShopString s = getById(shopStringId);
        if (s == null) return;
        int reserved = s.getReservedQuantity() != null ? s.getReservedQuantity() : 0;
        s.setReservedQuantity(reserved + quantity);
        updateById(s);
    }

    @Override
    @Transactional
    public void releaseStock(Long shopStringId, int quantity) {
        ShopString s = getById(shopStringId);
        if (s == null) return;
        int reserved = s.getReservedQuantity() != null ? s.getReservedQuantity() : 0;
        s.setReservedQuantity(Math.max(0, reserved - quantity));
        updateById(s);
    }

    @Override
    @Transactional
    public void deductStock(Long shopStringId, int quantity) {
        ShopString s = getById(shopStringId);
        if (s == null) return;
        int stock = s.getStock() != null ? s.getStock() : 0;
        int reserved = s.getReservedQuantity() != null ? s.getReservedQuantity() : 0;
        s.setStock(Math.max(0, stock - quantity));
        s.setReservedQuantity(Math.max(0, reserved - quantity));
        updateById(s);
    }

    @Override
    public IPage<ShopString> pageStock(Page<ShopString> page, boolean warningOnly, Long tenantId) {
        LambdaQueryWrapper<ShopString> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) qw.eq(ShopString::getTenantId, tenantId);
        if (warningOnly) {
            // (stock - reserved) <= threshold
            qw.apply("(COALESCE(stock, 0) - COALESCE(reserved_quantity, 0)) <= threshold");
        }
        qw.orderByDesc(ShopString::getUpdatedAt);
        return page(page, qw);
    }

    @Override
    @Transactional
    public void updateAfterCheck(Long shopStringId, int actualQuantity) {
        ShopString s = getById(shopStringId);
        if (s == null) return;
        s.setStock(actualQuantity);
        s.setLastCheckTime(java.time.LocalDateTime.now());
        updateById(s);
    }
}
