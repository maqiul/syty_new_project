package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 系统用户实体
 */
@Data
@TableName("sys_user")
public class SysUserEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID */
    private Long tenantId;
    /** 用户名 (登录账号) */
    private String username;
    /** 密码 (BCrypt 加密) */
    private String password;
    /** 真实姓名 */
    private String realName;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 头像URL */
    private String avatar;
    /** 状态: 0-停用, 1-正常 */
    private Integer status;
    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
