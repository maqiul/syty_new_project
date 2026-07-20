package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.common.BizException;
import com.syty.entity.StringingOrder;
import com.syty.mapper.StringingOrderMapper;
import com.syty.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
@Slf4j
@Service
@RequiredArgsConstructor
public class StringingOrderServiceImpl extends ServiceImpl<StringingOrderMapper, StringingOrder> implements StringingOrderService {
    private final StockService stockService;
    private final PaymentService paymentService;
    private final CommissionService commissionService;
    private final PunchCardService punchCardService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long orderId, BigDecimal payAmount, String payMethod) {
        // 1. 查询订单
        StringingOrder order = this.getById(orderId);
        if (order == null) {
            throw new BizException("订单不存在，orderId=" + orderId);
        }
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            throw new BizException("订单状态不允许完成，当前状态" + order.getStatus());
        }
        
        // 2. 扣减主线库存
        if (order.getMainStringId() != null) {
            stockService.deductStock(order.getShopId(), order.getMainStringId(), 1, orderId, order.getOrderNo());
        }
        
        // 3. 扣减横线库存
        if (order.getCrossStringId() != null) {
            stockService.deductStock(order.getShopId(), order.getCrossStringId(), 1, orderId, order.getOrderNo());
        }
        
        // === V1.7 次卡扣减 ===
        if (Boolean.TRUE.equals(order.getUsePunchCard()) && order.getPunchCardId() != null) {
            try {
                // 修正 V1.8：传入 customerId 而非 playerId
                punchCardService.deduct(order.getPunchCardId(), orderId, order.getTenantId(), order.getCustomerId());
                log.info("[次卡] 扣次成功: orderId={}, cardId={}", orderId, order.getPunchCardId());
            } catch (BizException e) {
                log.error("[次卡] 扣次失败 (订单状态仍更新为完成): orderId={}, error={}", orderId, e.getMessage());
                // 可以在这里标记订单为“次卡扣费异常”状态，或者仅仅记录日志让前台处理
            }
        }
        
        // 4. 记录支付
        if (payAmount != null && payAmount.compareTo(BigDecimal.ZERO) > 0 && payMethod != null) {
            paymentService.recordPayment(orderId, payAmount, payMethod);
        }
        
        // 5. 计算提成
        commissionService.calculateCommission(orderId);
        
        // 6. 更新订单状态为已完成
        StringingOrder update = new StringingOrder();
        update.setId(orderId);
        update.setStatus(2); // 2 = 已完成
        this.updateById(update);
        log.info("订单完成: orderId={}, orderNo={}", orderId, order.getOrderNo());
    }
}
