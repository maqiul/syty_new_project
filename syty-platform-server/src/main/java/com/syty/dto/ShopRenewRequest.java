package com.syty.dto;

import lombok.Data;

@Data
public class ShopRenewRequest {
    /**
     * 续费天数
     * 如果为 0 或不传，则后端自动按所选套餐的默认天数计算
     */
    private Integer days;
    
    /**
     * 新套餐 ID (支持续费时升降级)
     * 如果不传则保持原套餐
     */
    private Long packageId;
}
