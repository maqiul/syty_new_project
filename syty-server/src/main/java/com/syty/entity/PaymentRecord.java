package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 支付流水表
 * 每一笔收款/还款都有记录
 */
@Data
@TableName("payment_record")
public class PaymentRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long orderId;
    private String orderNo;
    private Long playerId;
    private String playerName;
    // 金额
    private BigDecimal amount;           // 本次支付金额
    private String payMethod;            // 支付方式: CASH/WECHAT/ALIPAY/BANK/CREDIT
    // 支付后状态
    private BigDecimal paidAmountAfter;  // 支付后该订单累计已付金额
    private String payStatusAfter;       // 支付后状态
    // 操作信息
    private Long operatorId;             // 收款人ID
    private String operatorName;         // 收款人姓名
    private String remark;               // 备注
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
