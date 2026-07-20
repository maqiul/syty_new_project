package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("sys_role_permission")
public class SysRolePermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleCode;
    private String permissionCode;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
