package com.syty.service.impl;
import com.syty.common.BizException;
import com.syty.common.TenantContext;
import com.syty.entity.PaymentRecord;
import com.syty.entity.StringingOrder;
import com.syty.mapper.PaymentRecordMapper;
import com.syty.mapper.StringingOrderMapper;
import com.syty.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 财务流水 Service 实现
 *
 * 记录逻辑：
 * 1. 查询订单，校验订单存在性和金额
 * 2. 向payment_record 表插入收款记录
 * 3. 更新订单paid_amount 和pay_status
 *    - paid_amount >= actual_amount → PAID (已结清)
 *    - paid_amount > 0 且 < actual_amount → PARTIAL (部分支付)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRecordMapper paymentRecordMapper;
    private final StringingOrderMapper stringingOrderMapper;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPayment(Long orderId, BigDecimal amount, String method) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("支付金额必须大于0");
        }
        if (method == null || method.isBlank()) {
            throw new BizException("支付方式不能为空");
        }
        // 1. 查询订单
        StringingOrder order = stringingOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在，orderId=" + orderId);
        }
        BigDecimal actualAmount = order.getActualAmount();
        if (actualAmount == null) {
            actualAmount = order.getTotalPrice();
        }
        if (actualAmount == null) {
            throw new BizException("订单应收金额为空，无法记录支付");
        }
        BigDecimal currentPaid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaidAmount = currentPaid.add(amount);
        // 2. 计算支付后状态
        String newPayStatus;
        if (newPaidAmount.compareTo(actualAmount) >= 0) {
            newPayStatus = "PAID";
        } else {
            newPayStatus = "PARTIAL";
        }
        // 3. 插入支付流水记录
        PaymentRecord record = new PaymentRecord();
        record.setTenantId(TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
        record.setOrderId(orderId);
        record.setOrderNo(order.getOrderNo());
        record.setPlayerId(order.getPlayerId());
        record.setPlayerName(order.getPlayerName());
        record.setAmount(amount);
        record.setPayMethod(method);
        record.setPaidAmountAfter(newPaidAmount);
        record.setPayStatusAfter(newPayStatus);
        record.setOperatorId(TenantContext.getUserId());
        record.setOperatorName(TenantContext.getUsername());
        record.setRemark("订单支付");
        paymentRecordMapper.insert(record);
        // 4. 更新订单支付信息
        StringingOrder update = new StringingOrder();
        update.setId(orderId);
        update.setPaidAmount(newPaidAmount);
        update.setPayStatus(newPayStatus);
        update.setPayMethod(method);
        update.setPayTime(LocalDateTime.now());
        stringingOrderMapper.updateById(update);
        log.info("支付记录成功: orderId={}, amount={}, method={}, paidAfter={}, status={}",
                orderId, amount, method, newPaidAmount, newPayStatus);
    }
}
