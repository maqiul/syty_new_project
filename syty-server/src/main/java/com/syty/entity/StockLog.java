package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 库存流水字
 * 所有库存变动必须经过此表记字
 */
@Data
@TableName("stock_log")
public class StockLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long shopId;
    private Long stringId;
    // 变动信息
    private String changeType;     // PURCHASE_IN-采购入库 ORDER_OUT-订单扣减 MANUAL_ADJUST-手动调整 RETURN_IN-退货入字TRANSFER-调拨
    private Integer quantity;      // 变动数量(正数为入字负数为出字
    private Integer beforeQuantity; // 变动前库字
    private Integer afterQuantity;  // 变动后库字
    // 关联业务
    private Long orderId;          // 关联订单ID
    private String orderNo;        // 关联订单
    // 操作信息
    private Long operatorId;       // 操作人ID
    private String operatorName;   // 操作人姓字
    private String remark;         // 备注
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
