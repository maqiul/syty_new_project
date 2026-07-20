package com.syty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 128, message = "密码长度不能超过128个字符")
    private String password;

    /**
     * 租户编码（可选）
     * - 有值：员工登录，走 tenantCode 对应的 schema
     * - 无值：管理员登录，走 public schema
     */
    @Size(max = 64, message = "租户编码长度不能超过64个字符")
    private String tenantCode;
}
