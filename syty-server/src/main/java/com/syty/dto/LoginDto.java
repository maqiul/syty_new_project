package com.syty.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class LoginDto {
    /**
     * 租户编码 (非必填，超管登录可留空)
     */
    private String tenantCode;
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
}
