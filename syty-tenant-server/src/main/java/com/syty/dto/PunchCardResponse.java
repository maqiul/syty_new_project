package com.syty.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 次卡响应 DTO (给 H5 用)
 */
@Data
public class PunchCardResponse {

    private Long id;

    private String cardType;

    private Integer remainingCount;

    private LocalDateTime expireTime;
}
