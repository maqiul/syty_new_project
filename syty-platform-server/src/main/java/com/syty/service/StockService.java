package com.syty.service;
/**
 * 库存管理 Service
 * 负责线材库存扣减和流水记字
 */
public interface StockService {
    /**
     * 预留库存（下单时调用）
     */
    void reserveStock(Long shopId, Long stringId, int quantity, Long orderId, String orderNo);

    /**
     * 扣减库存（订单完成时调用，扣除已预留部分）
     */
    void deductStock(Long shopId, Long stringId, int quantity, Long orderId, String orderNo);

    /**
     * 释放库存（订单取消时调用，释放已预留部分）
     */
    void releaseStock(Long shopId, Long stringId, int quantity, Long orderId, String orderNo);
}
