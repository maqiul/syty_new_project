package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("tennis_shop_string")
public class TennisShopString {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long shopId;
    private Long stringId;
    private BigDecimal price;
    private Integer stock;
    private Integer minStockAlert;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
