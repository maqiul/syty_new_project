package com.syty.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 打印静态资源实字
 */
@Data
@TableName("print_resource")
public class PrintResource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String shopId; // 空字符串表示全局资源
    private String resourceKey; // LOGO, QR_PAY
    private String resourceUrl;
    private String fileName; // 原始文件名
    private String mimeType; // MIME 类型
    private Long fileSize; // 文件大小 (字节)
    private Integer status; // 1: 启用 0: 停用
    private Long tenantId; // 租户ID
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
