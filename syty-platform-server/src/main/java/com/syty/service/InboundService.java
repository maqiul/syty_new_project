package com.syty.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.common.BizException;
import com.syty.common.TenantContext;
import com.syty.dto.InboundRequest;
import com.syty.entity.*;
import com.syty.mapper.*;
import com.syty.mapper.ShopStringMapper;
import com.syty.mapper.StringInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 入库服务
 * <p>
 * V1.4 核心逻辑:
 * - 接收入库单 -> 写入 inbound_order + inbound_item
 * - 直接增加 shop_string 库存 (无需审核)
 * - 库存存在则累加, 不存在则新建记录
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundService {

    private final InboundOrderMapper inboundOrderMapper;
    private final InboundItemMapper inboundItemMapper;
    private final ShopStringMapper shopStringMapper;
    private final StringInfoMapper stringInfoMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 创建入库单并增加库存 (无需审核)
     *
     * @param request 入库请求
     * @return 入库单实体
     */
    @Transactional(rollbackFor = Exception.class)
    public InboundOrder createInbound(InboundRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long operatorId = TenantContext.getUserId();

        log.info("创建入库单: tenantId={}, shopId={}, operatorId={}, 明细数={}",
                tenantId, request.getShopId(), operatorId, request.getItems().size());

        // Step 1: 预计算汇总 (先校验所有线材)
        int totalQuantity = 0;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (InboundRequest.InboundItemDTO itemDTO : request.getItems()) {
            StringInfo stringInfo = stringInfoMapper.selectById(itemDTO.getStringId());
            if (stringInfo == null) {
                throw new BizException("线材不存在: id=" + itemDTO.getStringId());
            }
            if (itemDTO.getQuantity() <= 0) {
                throw new BizException("入库数量必须大于0");
            }

            totalQuantity += itemDTO.getQuantity();
            totalCost = totalCost.add(itemDTO.getCostPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
        }

        // Step 2: 插入入库单主表 (先拿到 orderId)
        InboundOrder order = new InboundOrder();
        order.setTenantId(tenantId);
        order.setShopId(request.getShopId());
        order.setOperatorId(operatorId);
        order.setTotalQuantity(totalQuantity);
        order.setTotalCost(totalCost);
        order.setRemark(request.getRemark());
        inboundOrderMapper.insert(order);

        Long orderId = order.getId();

        // Step 3: 插入明细 + 增加库存
        for (InboundRequest.InboundItemDTO itemDTO : request.getItems()) {
            // 3.1 写入明细
            InboundItem item = new InboundItem();
            item.setTenantId(tenantId);
            item.setOrderId(orderId);
            item.setStringId(itemDTO.getStringId());
            item.setQuantity(itemDTO.getQuantity());
            item.setCostPrice(itemDTO.getCostPrice());
            inboundItemMapper.insert(item);

            // 3.2 增加 shop_string 库存 (存在则累加, 不存在则新建)
            upsertShopString(tenantId, request.getShopId(), itemDTO.getStringId(), itemDTO.getQuantity());
        }

        log.info("入库单创建完成: orderId={}, totalQuantity={}, totalCost={}",
                orderId, totalQuantity, totalCost);

        return inboundOrderMapper.selectById(orderId);
    }

    /**
     * 更新门店线材库存 (存在则累加, 不存在则新建)
     */
    private void upsertShopString(Long tenantId, Long shopId, Long stringId, int quantity) {
        LambdaQueryWrapper<ShopString> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopString::getTenantId, tenantId)
               .eq(ShopString::getShopId, shopId)
               .eq(ShopString::getStringId, stringId);

        ShopString existing = shopStringMapper.selectOne(wrapper);

        if (existing != null) {
            // 累加库存
            existing.setStock(existing.getStock() + quantity);
            shopStringMapper.updateById(existing);
            log.info("更新库存: shopStringId={}, 原库存={}, 新库存={}",
                    existing.getId(), existing.getStock() - quantity, existing.getStock());
        } else {
            // 新建库存记录
            ShopString newStock = new ShopString();
            newStock.setTenantId(tenantId);
            newStock.setShopId(shopId);
            newStock.setStringId(stringId);
            newStock.setStock(quantity);
            newStock.setReservedQuantity(0);
            newStock.setMinStockAlert(0);
            shopStringMapper.insert(newStock);
            log.info("新建库存记录: shopId={}, stringId={}, 初始库存={}",
                    shopId, stringId, quantity);
        }
    }

    /**
     * 删除入库单并扣减库存 (事务回滚)
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteInbound(Long orderId) {
        log.info("撤销入库单: orderId={}", orderId);
        InboundOrder order = inboundOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("入库单不存在");
        }

        // 1. 获取明细
        LambdaQueryWrapper<InboundItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InboundItem::getOrderId, orderId);
        List<InboundItem> items = inboundItemMapper.selectList(itemWrapper);

        // 2. 扣减库存
        for (InboundItem item : items) {
            LambdaQueryWrapper<ShopString> stockWrapper = new LambdaQueryWrapper<>();
            stockWrapper.eq(ShopString::getTenantId, order.getTenantId())
                    .eq(ShopString::getShopId, order.getShopId())
                    .eq(ShopString::getStringId, item.getStringId());
            ShopString stock = shopStringMapper.selectOne(stockWrapper);

            if (stock != null) {
                int newStock = stock.getStock() - item.getQuantity();
                if (newStock < 0) {
                    log.warn("撤销入库导致库存为负: stringId={}, 扣减={}, 当前={}", item.getStringId(), item.getQuantity(), stock.getStock());
                }
                stock.setStock(newStock);
                shopStringMapper.updateById(stock);
            } else {
                log.error("库存记录不存在，无法撤销: stringId={}", item.getStringId());
            }
        }

        // 3. 删除明细和主单
        inboundItemMapper.delete(itemWrapper);
        inboundOrderMapper.deleteById(orderId);
        log.info("入库单撤销完成: orderId={}", orderId);
    }

    /**
     * 查询入库单列表
     */
    public List<InboundOrder> listInbound() {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrder::getTenantId, tenantId)
               .orderByDesc(InboundOrder::getCreatedAt);
        return inboundOrderMapper.selectList(wrapper);
    }

    /**
     * 查询入库单详情
     */
    public InboundOrder getOrderById(Long id) {
        return inboundOrderMapper.selectById(id);
    }
}
