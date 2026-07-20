package com.syty.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
/**
 * 当前用户信息响应 (GET /api/v1/auth/me)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    /** 用户ID */
    private Long userId;
    /** 用户名 */
    private String username;
    /** 真实姓名 */
    private String realName;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 头像URL */
    private String avatar;
    /** 角色编码列表, 如 ["SUPER_ADMIN", "SHOP_ADMIN"] */
    private List<String> roleCodes;
    /** 完整菜单树 */
    private List<MenuTreeVO> menus;
}
