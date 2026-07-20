package com.syty.dto;

import lombok.Data;

@Data
public class RenewPackageRequest {
    private Long packageId;
    private Integer durationDays; // 前端不传则使用套餐默认
    private String operateType;   // RENEW, COMPENSATE, ADJUST
    private String remark;        // 备注
}
