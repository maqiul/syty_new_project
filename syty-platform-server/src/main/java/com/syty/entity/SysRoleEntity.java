package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 系统角色实体
 */
@Data
@TableName("sys_role")
public class SysRoleEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID */
    private Long tenantId;
    /** 角色编码 (唯一标识) */
    private String roleCode;
    /** 角色名称 */
    private String roleName;
    /** 角色描述 */
    private String description;
    /** 数据权限: 1-全部, 2-本部门, 3-仅本人 */
    private Integer dataScope;
    /** 状态: 0-停用, 1-正常 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
