package com.syty.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
/**
 * 注册打印机请求 DTO
 */
@Data
public class PrinterRegisterRequest {
    @NotBlank(message = "门店ID不能为空")
    private String shopId;
    @NotBlank(message = "机器ID不能为空")
    private String machineId;
    @NotEmpty(message = "打印机名称列表不能为空")
    private List<String> printerNames;
    /** 默认打印机名称 (可选, 不传则以第一个为准) */
    private String defaultPrinterName;
}
