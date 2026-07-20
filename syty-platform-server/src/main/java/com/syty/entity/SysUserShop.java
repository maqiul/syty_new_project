package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("sys_user_shop")
public class SysUserShop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long shopId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long tenantId;
}
