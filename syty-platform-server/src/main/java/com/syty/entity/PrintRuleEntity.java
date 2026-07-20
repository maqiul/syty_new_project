package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 打印规则实体
 *
 * V1.2 变更: 增加 tenant_id 字段，支持租户隔离
 */
@Data
@TableName("print_rule")
public class PrintRuleEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID (V1.2 新增) */
    private Long tenantId;
    private String shopId;
    private String machineId;
    /** 单据类型: RECEIPT-小票, ORDER-订单, LABEL-标签, INVOICE-发票 */
    private String docType;
    private String printerName;
    /** 打印模板路径 */
    private String templatePath;
    /** 纸张规格: 80mm/58mm/A4 */
    private String paperSize;
    /** 打印份数 */
    private Integer copies;
    /** 是否启用: 0-否, 1-是 */
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
