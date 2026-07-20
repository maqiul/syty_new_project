package com.syty.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * 打印回执上报请求 DTO
 * C# 客户端调用 POST /api/print-task/report
 */
@Data
public class PrintTaskReportRequest {
    @NotBlank(message = "任务ID不能为空")
    private String taskId;
    /** 订单ID */
    private String orderId;
    /** 打印机ID */
    private String printerId;
    /** 打印状态: SUCCESS-成功, FAIL-失败 */
    @NotBlank(message = "打印状态不能为空")
    private String status;
    /** 错误信息 (失败时填写) */
    private String errorMsg;
}
