package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 打印机配置实体
 *
 * V1.2 变更: 增加 tenant_id 字段，支持租户隔离
 */
@Data
@TableName("printer_config")
public class PrinterConfigEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID (V1.2 新增) */
    private Long tenantId;
    private String shopId;
    private String machineId;
    private String printerName;
    /** 是否默认打印机: 0-否, 1-是 */
    private Integer isDefault;
    /** 状态: 0-禁用, 1-启用 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
