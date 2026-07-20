package com.syty.service;
/**
 * 提成计算 Service
 * 根据 commission_rule 表配置的规则计算穿线师提成
 */
public interface CommissionService {
    /**
     * 计算订单提成并更新订单状态
     *
     * @param orderId 订单ID
     * @throws com.syty.common.BizException 规则配置异常时抛出
     */
    void calculateCommission(Long orderId);
}
