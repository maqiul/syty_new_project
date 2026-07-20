package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入库单明细实体
 */
@Data
@TableName("inbound_item")
public class InboundItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 入库单ID */
    private Long orderId;

    /** 线材ID */
    private Long stringId;

    /** 线材名称 (冗余, 避免JOIN) */
    private String stringName;

    /** 入库数量 */
    private Integer quantity;

    /** 入库单价 (成本价) */
    private BigDecimal costPrice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
