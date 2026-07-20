package com.syty.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_shop")
public class SysShop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String tenantCode;
    private String shopCode;
    private String shopName;
    private Long packageId;
    private LocalDateTime expiredAt;
    private String status; // ACTIVE, BUSY, EXPIRED, SUSPENDED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
