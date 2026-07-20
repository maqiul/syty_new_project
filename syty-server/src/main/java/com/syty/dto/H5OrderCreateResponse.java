package com.syty.dto;

import lombok.Data;

/**
 * H5 下单响应 DTO
 */
@Data
public class H5OrderCreateResponse {

    private Long orderId;
    private String orderNo;
    private String status;
    private String warning;
}
