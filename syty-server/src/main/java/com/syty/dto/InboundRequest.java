package com.syty.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 入库请求 DTO
 */
@Data
public class InboundRequest {

    /** 入库门店ID */
    @NotNull(message = "门店ID不能为空")
    private Long shopId;

    /** 备注 */
    private String remark;

    /** 入库明细列表 */
    @NotEmpty(message = "入库明细不能为空")
    @Valid
    private List<InboundItemDTO> items;

    @Data
    public static class InboundItemDTO {
        /** 线材ID */
        @NotNull(message = "线材ID不能为空")
        private Long stringId;

        /** 入库数量 */
        @NotNull(message = "入库数量不能为空")
        private Integer quantity;

        /** 入库单价 (成本价) */
        @NotNull(message = "成本价不能为空")
        private BigDecimal costPrice;
    }
}
