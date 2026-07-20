package com.syty.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.syty.entity.StringingOrder;
import java.math.BigDecimal;
public interface StringingOrderService extends IService<StringingOrder> {
    /**
     * V1.1 订单完成：扣库存 + 记录支付 + 计算提成 + 更新状态
     * 整个流程在一个事务中执行
     *
     * @param orderId    订单ID
     * @param payAmount  支付金额（可为null）
     * @param payMethod  支付方式（可为null）
     */
    void completeOrder(Long orderId, BigDecimal payAmount, String payMethod);
}
