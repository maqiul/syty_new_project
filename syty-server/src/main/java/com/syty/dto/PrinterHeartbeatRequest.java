package com.syty.dto;
import lombok.Data;
import java.util.Map;
/**
 * 打印机心跳请求DTO
 */
@Data
public class PrinterHeartbeatRequest {
    private String machineId;
    private Long shopId;
    /**
     * Key: 打印机名字 Value: 状态(ONLINE, OFFLINE, ERROR)
     */
    private Map<String, String> printerStatusMap;
}
