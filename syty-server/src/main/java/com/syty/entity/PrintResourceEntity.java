package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 打印资源实体 (静态图片: Logo、收款码等)
 *
 * V1.2 变更: 增加 tenant_id 字段，支持租户隔离
 */
@Data
@TableName("print_resource")
public class PrintResourceEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID (V1.2 新增) */
    private Long tenantId;
    /** 门店ID (空字符串表示全局资源) */
    private String shopId;
    /** 资源标识: LOGO, QR_CODE_PAY, QR_CODE_MINI 等 */
    private String resourceKey;
    /** 资源 URL (MinIO/OSS 地址) */
    private String resourceUrl;
    /** 原始文件名 */
    private String fileName;
    /** MIME 类型 */
    private String mimeType;
    /** 文件大小 (字节) */
    private Long fileSize;
    /** 状态: 0-删除, 1-正常 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
