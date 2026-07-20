package com.syty.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.InventoryCheck;
import com.syty.entity.InventoryCheckItem;
import com.syty.entity.ShopString;
import com.syty.mapper.InventoryCheckMapper;
import com.syty.service.InventoryCheckService;
import com.syty.service.ShopStringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryCheckServiceImpl extends ServiceImpl<InventoryCheckMapper, InventoryCheck> implements InventoryCheckService {

    private final ShopStringService shopStringService;
    private final com.syty.mapper.InventoryCheckItemMapper itemMapper;

    @Override
    @Transactional
    public InventoryCheck createCheck(Long tenantId, Long shopId) {
        InventoryCheck check = new InventoryCheck();
        check.setTenantId(tenantId);
        check.setShopId(shopId);
        check.setCheckType("FULL");
        check.setStatus("DRAFT");
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String checkNo = "CK-" + dateStr + "-" + String.format("%04d", (int)(Math.random() * 10000));
        check.setCheckNo(checkNo);
        save(check);
        return check;
    }

    @Override
    @Transactional
    public void submitItems(Long checkId, List<InventoryCheckItem> items) {
        for (InventoryCheckItem item : items) {
            item.setCheckId(checkId);
            item.setDiffQuantity((item.getActualQuantity() != null ? item.getActualQuantity() : 0) -
                                (item.getBookQuantity() != null ? item.getBookQuantity() : 0));
            // 删除旧记录再插入（简化处理）
            itemMapper.delete(new LambdaQueryWrapper<InventoryCheckItem>().eq(InventoryCheckItem::getCheckId, checkId).eq(InventoryCheckItem::getStringId, item.getStringId()));
            itemMapper.insert(item);
        }
    }

    @Override
    public List<InventoryCheckItem> getCheckItems(Long checkId) {
        return itemMapper.selectList(
            new LambdaQueryWrapper<InventoryCheckItem>()
                .eq(InventoryCheckItem::getCheckId, checkId)
                .orderByAsc(InventoryCheckItem::getStringId));
    }

    @Override
    @Transactional
    public void confirmCheck(Long checkId) {
        InventoryCheck check = getById(checkId);
        if (check == null) return;
        
        // 获取所有明细
        List<InventoryCheckItem> items = itemMapper.selectList(
            new LambdaQueryWrapper<InventoryCheckItem>().eq(InventoryCheckItem::getCheckId, checkId));
        
        for (InventoryCheckItem item : items) {
            if (item.getActualQuantity() != null) {
                shopStringService.updateAfterCheck(item.getStringId(), item.getActualQuantity());
            }
        }
        
        check.setStatus("COMPLETED");
        check.setCheckTime(java.time.LocalDateTime.now());
        updateById(check);
    }

    @Override
    public IPage<InventoryCheck> pageCheck(Page<InventoryCheck> page, Long tenantId) {
        LambdaQueryWrapper<InventoryCheck> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) qw.eq(InventoryCheck::getTenantId, tenantId);
        qw.orderByDesc(InventoryCheck::getCreatedAt);
        return page(page, qw);
    }
}
