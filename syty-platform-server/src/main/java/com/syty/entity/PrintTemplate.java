package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("sys_print_template")
public class PrintTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long shopId;
    private String name;
    private String content; // V1.4 新增 JSON 模板内容
    private Integer paperWidth;
    private Integer paperHeight;
    private Integer fontSize;
    private Integer showLogo;
    private Integer showQrcode;
    private String fieldsJson;
    private Integer status = 1; // V1.5 新增状态字段
    private Integer isDefault;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
