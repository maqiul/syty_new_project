package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String username;
    private String password;
    private String realName;
    private String phone;
    @TableField(exist = false)
    private String role;
    @TableField(exist = false)
    private Long shopId;
    @TableField(exist = false)
    private String email;
    @TableField(exist = false)
    private String avatar;
    private Integer status;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
