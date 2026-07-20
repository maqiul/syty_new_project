package com.syty.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 库存预留结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationResult {
    /** 是否预留成功 */
    private boolean success;
    /** 预留前的可用库存 */
    private int availableBefore;
    /** 预留后的可用库存 */
    private int availableAfter;
    /** 预留数量 */
    private int reservedAmount;
    /** 消息 */
    private String message;
}
