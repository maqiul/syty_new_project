package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员认证索引表 (位于 public schema)
 * 
 * 该表存储所有租户管理员的认证信息，跨租户共享。
 * 位于 public schema，不受租户插件影响。
 */
@Data
@TableName("public.sys_user_auth_index")
public class SysUserAuthIndex {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 租户编码 (用于登录后路由到对应 tenant schema) */
    private String tenantCode;
    
    /** 管理员用户名 */
    private String username;
    
    /** BCrypt 加密密码 */
    private String password;
    
    /** 真实姓名 */
    private String realName;
    
    /** 手机号 */
    private String phone;
    
    /** 邮箱 */
    private String email;
    
    /** 状态: 1-启用, 0-禁用 */
    private Integer status;
    
    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
