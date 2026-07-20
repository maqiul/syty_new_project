package com.syty.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    /** Sa-Token Token 值 */
    private String token;
    /** Token 名称 (默认 satoken) */
    private String tokenName;
    /** 用户ID */
    private Long userId;
    /** 用户名 */
    private String username;
}
