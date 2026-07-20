package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("stringing_order")
public class StringingOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String orderNo;
    private String shortOrderNo;
    private Long shopId;
    private Long playerId;      // 赛事关联 (可选)
    private Long customerId;    // 门店客户关联 (主键)
    private Long racketId;
    private Long mainStringId;
    private Long crossStringId;
    private String playerPhone;
    private BigDecimal mainTension;
    private BigDecimal crossTension;
    private BigDecimal mainPrice;
    private BigDecimal crossPrice;
    private BigDecimal laborPrice;
    private BigDecimal totalPrice;
    private Integer status;
    private String remark;
    private String source;
    private Integer printed;
    private Integer printCount;
    private String orderPhotoUrl; // 穿线现场照片
    // === V1.1 新增：运动类字===
    private String sportType;   // 运动类型: BADMINTON-羽毛字TENNIS-网球
    // === V1.1 新增：支付相字===
    private String payStatus;        // 支付状字 UNPAID-未付 PARTIAL-部分支付 PAID-已结字CREDIT-挂账
    private BigDecimal paidAmount;   // 已支付金字
    private String payMethod;        // 支付方式: CASH-现金 WECHAT-微信 ALIPAY-支付字BANK-银行字CREDIT-挂账/月结
    private LocalDateTime payTime;   // 最后支付时字
    private BigDecimal discountAmount; // 优惠金额
    private BigDecimal actualAmount;   // 实际应收金额(total_price - discount_amount)
    // === V1.1 新增：穿线师绩效 ===
    private Long stringerId;      // 穿线师ID（关联sys_user）
    private String stringerName;  // 穿线师姓名（冗余，避免JOIN）
    private BigDecimal commission; // 提成金额

    // === V1.7 电子次卡 ===
    private Boolean usePunchCard; // 是否使用次卡
    private Long punchCardId;     // 使用的次卡ID

    // 以下为关联查询展示字段（非数据库字段）
    @TableField(exist = false)
    private String shopName;
    @TableField(exist = false)
    private String playerName;
    @TableField(exist = false)
    private String racketName;
    @TableField(exist = false)
    private String mainStringName;
    @TableField(exist = false)
    private String crossStringName;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
