package com.syty.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;
/**
 * 拉取打印配置响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintSetupResponse {
    /** 打印机列表 */
    private List<PrinterInfo> printers;
    /** 打印规则: key=单据类型, value=规则详情 */
    private Map<String, PrintRuleInfo> rules;
    /** 静态资源: key=资源标识, value=URL */
    private Map<String, String> resources;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrinterInfo {
        private String printerName;
        private boolean isDefault;
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrintRuleInfo {
        private String docType;
        private String printerName;
        private String templatePath;
        private String paperSize;
        private int copies;
    }
}
