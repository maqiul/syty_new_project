package com.syty.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * H5 下单请求 DTO
 */
@Data
public class H5OrderCreateRequest {

    private String phone;
    private String racketModel;
    private BigDecimal mainPounds;
    private BigDecimal crossPounds;
    private String knotType;

    // V1.7 次卡字段
    private Boolean usePunchCard;
    private Long punchCardId;
}
