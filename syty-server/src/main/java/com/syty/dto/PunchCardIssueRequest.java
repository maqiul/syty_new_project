package com.syty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 售卡请求 DTO
 */
@Data
public class PunchCardIssueRequest {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotNull(message = "租户 ID 不能为空")
    private Long tenantId;

    /** 卡类型: TEN_TIMES, TWENTY_TIMES */
    @NotBlank(message = "卡种不能为空")
    private String cardType;

    /** 有效期天数，默认 90 */
    private Integer validDays;
}
