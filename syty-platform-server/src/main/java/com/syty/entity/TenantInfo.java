package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台端全局租户信息表（public schema）
 */
@Data
@TableName("tenant")
public class TenantInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户编码 */
    @TableField("code")
    private String tenantCode;

    /** 租户名称 */
    private String name;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String phone;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;

    /** 关联套餐 ID */
    private Long packageId;

    /** 套餐到期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime packageExpiredAt;

    /** 套餐状态: ACTIVE / EXPIRED / SUSPENDED */
    private String packageStatus;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
