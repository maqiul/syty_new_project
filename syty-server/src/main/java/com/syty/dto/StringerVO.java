package com.syty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 穿线师用户信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StringerVO {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;
}
