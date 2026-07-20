package com.syty.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 打印规则实体
 */
@Data
@TableName("print_rule")
public class PrintRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String shopId;
    private String machineId; // 机器标识
    private String docType; // RECEIPT, TAG
    private String printerName;
    private String templatePath;
    private String paperSize; // 纸张大小 (A4/A5/58mm/80mm)
    private Integer copies;
    private Boolean isEnabled;
    private Integer sortOrder;
    private Long tenantId; // 租户ID
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
