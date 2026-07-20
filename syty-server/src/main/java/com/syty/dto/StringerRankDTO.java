package com.syty.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StringerRankDTO {
    private Long stringerId;
    private String stringerName;
    private Long orderCount;
    private BigDecimal totalFee;
    private Double avgDuration;
    private Integer rank;
}
