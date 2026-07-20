package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("inventory_check")
public class InventoryCheck {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private String checkNo;
    private String checkType;
    private String status;
    private Long operatorId;
    private LocalDateTime checkTime;
    private String remark;
    private Long tenantId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
