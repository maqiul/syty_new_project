package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 库存实体
 * 支持库存预留 (reserved_stock) 机制
 */
@Data
@TableName("inventory_stock")
public class InventoryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 商品/耗材编号 */
    private String skuCode;
    /** 商品名称 */
    private String skuName;
    /** 总库存量 */
    private Integer totalStock;
    /** 已预留库存 (接单时预扣) */
    private Integer reservedStock;
    /** 可用库存 = totalStock - reservedStock */
    private Integer availableStock;
    /** 状态: 0-停用, 1-正常 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
