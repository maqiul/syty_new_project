package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 平台管理员用户实体
 * 独立于租户用户，不存储 tenant_id
 */
@Data
@TableName("sys_platform_user")
public class SysPlatformUser {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    
    /** 角色编码 (如 SUPER_ADMIN) */
    private String role;
    
    /** 状态: 1-正常, 0-停用 */
    private Integer status;
    
    @TableLogic
    private Integer deleted;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}