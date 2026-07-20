package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("shop_string")
public class ShopString {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long shopId;
    private Long stringId;
    private BigDecimal price;
    private Integer stock;
    @TableField("reserved_quantity")
    private Integer reservedQuantity;
    @TableField("threshold")
    private Integer threshold;
    @TableField("unit")
    private String unit;
    @TableField("last_check_time")
    private LocalDateTime lastCheckTime;
    @TableField("min_stock_alert")
    private Integer minStockAlert;
    // === V1.1 新增 ===
    private Long updatedBy;  // 最后修改人
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
