package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String icon;
    @TableField(exist = false)
    private String permissionCode;
    private Integer sortOrder;
    /** 0=目录 1=菜单 */
    private Integer type;
    private Integer hidden;
    private Long tenantId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
