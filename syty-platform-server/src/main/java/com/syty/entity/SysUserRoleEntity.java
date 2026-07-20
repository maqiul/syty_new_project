package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 用户-角色关联实体
 */
@Data
@TableName("sys_user_role")
public class SysUserRoleEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 角色ID */
    private Long roleId;
    /** 租户ID */
    private Long tenantId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
