package com.syty.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tenant_package_log")
public class TenantPackageLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long packageId;
    private String operateType; // RENEW, COMPENSATE, ADJUST
    private Integer durationDays;
    private Integer gapDays;
    private Integer shrinkDays;
    private String remark;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime beforeExpiredAt;
    private LocalDateTime afterExpiredAt;
    private LocalDateTime createdAt;
}
