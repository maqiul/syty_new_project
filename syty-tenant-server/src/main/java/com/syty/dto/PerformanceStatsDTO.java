package com.syty.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PerformanceStatsDTO {
    private Long totalOrders;
    private BigDecimal totalFee;
    private Double avgDuration; // minutes
    private Long newMembers;
}
