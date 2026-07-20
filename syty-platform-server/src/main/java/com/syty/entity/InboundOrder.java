package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入库单主表实体
 */
@Data
@TableName("inbound_order")
public class InboundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 入库门店ID */
    private Long shopId;

    /** 操作人ID (穿线师) */
    private Long operatorId;

    /** 操作人姓名 (冗余) */
    private String operatorName;

    /** 入库总数量 */
    private Integer totalQuantity;

    /** 入库总成本 */
    private BigDecimal totalCost;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
