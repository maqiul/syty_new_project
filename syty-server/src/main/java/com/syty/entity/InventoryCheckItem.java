package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("inventory_check_item")
public class InventoryCheckItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long checkId;
    private Long stringId;
    private Integer bookQuantity;
    private Integer actualQuantity;
    private Integer diffQuantity; // DB generated, but we can compute in Java too
    private String reason;
    private Long tenantId;
}
