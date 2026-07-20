package com.syty.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.common.BizException;
import com.syty.common.TenantContext;
import com.syty.entity.ShopString;
import com.syty.entity.StockLog;
import com.syty.mapper.ShopStringMapper;
import com.syty.mapper.StockLogMapper;
import com.syty.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * 库存管理 Service 实现
 *
 * 扣减逻辑：
 * 1. 查询 shop_string 表，使用 SELECT ... FOR UPDATE 加行锁防并发
 * 2. 校验库存是否充足
 * 3. 扣减库存（带条件更新防止超卖）
 * 4. 写入 stock_log 流水记录（ORDER_OUT）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {
    private final StockLogMapper stockLogMapper;
    private final ShopStringMapper shopStringMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserveStock(Long shopId, Long stringId, int quantity, Long orderId, String orderNo) {
        if (quantity <= 0) {
            throw new BizException("预留数量必须大于0");
        }
        // 1. 查询库存记录（FOR UPDATE 行锁）
        ShopString shopString = shopStringMapper.selectForUpdate(shopId, stringId);
        if (shopString == null) {
            throw new BizException("店铺未配置该线材库存");
        }
        
        // 2. 校验可用库存 (stock - reserved_stock >= quantity)
        int reservedQuantity = shopString.getReservedQuantity();
        int currentStock = shopString.getStock();
        int available = currentStock - reservedQuantity;
        if (available < quantity) {
            throw new BizException("可用库存不足！当前可用: " + available + "，需求: " + quantity);
        }
        
        // 3. 增加预留
        int affectedRows = shopStringMapper.updateReservedStock(shopString.getId(), quantity);
        if (affectedRows <= 0) {
            throw new BizException("库存预留失败，可能并发冲突");
        }
        
        // 4. 记录流水
        writeLog(shopId, stringId, quantity, "RESERVE", orderId, orderNo, "接单预留");
        log.info("库存预留成功: shopId={}, stringId={}, 预留量={}", shopId, stringId, quantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long shopId, Long stringId, int quantity, Long orderId, String orderNo) {
        if (quantity <= 0) {
            throw new BizException("扣减数量必须大于0");
        }
        // 1. 查询库存记录（FOR UPDATE 行锁）
        ShopString shopString = shopStringMapper.selectForUpdate(shopId, stringId);
        if (shopString == null) {
            throw new BizException("店铺未配置该线材库存");
        }
        
        // 2. 校验总库存
        int currentStock = shopString.getStock();
        if (currentStock < quantity) {
            throw new BizException("库存不足！总库存: " + currentStock + "，需求: " + quantity);
        }

        // 3. 扣减库存 (stock -= quantity)
        // 注意：这里只扣减总库存。预留库存应该在“接单”时已经加了，
        // 现在“完成”时，预留库存也应该减去（因为不再预占，而是真正消耗了）。
        int affectedRows = shopStringMapper.updateStockWithCheck(
                shopString.getId(),
                -quantity,
                TenantContext.getUserId()
        );
        if (affectedRows <= 0) {
            throw new BizException("库存扣减失败");
        }
        
        // 4. 同步扣减预留库存 (reserved_stock -= quantity)
        shopStringMapper.updateReservedStock(shopString.getId(), -quantity);
        
        // 5. 记录流水
        writeLog(shopId, stringId, quantity, "ORDER_OUT", orderId, orderNo, "订单完成扣减");
        log.info("库存扣减成功: shopId={}, stringId={}, 扣减量={}", shopId, stringId, quantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseStock(Long shopId, Long stringId, int quantity, Long orderId, String orderNo) {
        if (quantity <= 0) {
            throw new BizException("释放数量必须大于0");
        }
        // 1. 查询库存记录（FOR UPDATE 行锁）
        ShopString shopString = shopStringMapper.selectForUpdate(shopId, stringId);
        if (shopString == null) {
            throw new BizException("店铺未配置该线材库存");
        }
        
        int reservedQuantity = shopString.getReservedQuantity();
        if (reservedQuantity < quantity) {
            throw new BizException("预留库存不足，无法释放");
        }
        
        // 2. 扣减预留库存
        int affectedRows = shopStringMapper.updateReservedStock(shopString.getId(), -quantity);
        if (affectedRows <= 0) {
            throw new BizException("库存释放失败");
        }
        
        // 3. 记录流水 (负数表示回补可用)
        writeLog(shopId, stringId, -quantity, "RELEASE", orderId, orderNo, "订单取消释放");
        log.info("库存释放成功: shopId={}, stringId={}, 释放量={}", shopId, stringId, quantity);
    }
    
    private void writeLog(Long shopId, Long stringId, int quantity, String type, Long orderId, String orderNo, String remark) {
        StockLog stockLog = new StockLog();
        stockLog.setTenantId(TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
        stockLog.setShopId(shopId);
        stockLog.setStringId(stringId);
        stockLog.setChangeType(type);
        stockLog.setQuantity(quantity);
        // 简化处理，before/after 可以在这里查，或者留给触发器/更复杂的逻辑
        // 这里暂时设为 0，实际项目中应精确计算
        stockLog.setBeforeQuantity(0);
        stockLog.setAfterQuantity(0);
        stockLog.setOrderId(orderId);
        stockLog.setOrderNo(orderNo);
        stockLog.setOperatorId(TenantContext.getUserId());
        stockLog.setOperatorName(TenantContext.getUsername());
        stockLog.setRemark(remark);
        stockLogMapper.insert(stockLog);
    }
}
