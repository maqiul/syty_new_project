package com.syty.service;
import java.math.BigDecimal;
/**
 * 财务流水 Service
 * 负责支付记录插入和订单支付状态更新
 */
public interface PaymentService {
    /**
     * 记录支付并更新订单支付状态
     *
     * @param orderId 订单ID
     * @param amount  本次支付金额
     * @param method  支付方式 (CASH/WECHAT/ALIPAY/BANK/CREDIT)
     * @throws com.syty.common.BizException 支付金额异常时抛字
     */
    void recordPayment(Long orderId, BigDecimal amount, String method);
}
