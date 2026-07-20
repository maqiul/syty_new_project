package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 打印策略路由实体 (V1.4 新增)
 */
@Data
@TableName("sys_print_policy")
public class PrintPolicyEntity {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 门店ID (NULL 代表公共策略) */
    private Long shopId;

    /** 场景: WALK_IN-散客, TOURNAMENT-赛事, ALL-全部 */
    private String scene;

    /** 运动类型: BADMINTON-羽毛球, TENNIS-网球, ALL-全部 */
    private String sportType;

    /** 单据类型: LABEL-标签, RECEIPT-回执 */
    private String docType;

    /** 关联模板ID */
    private Long templateId;

    /** 目标角色: FRONT_DESK-前台, STRINGING_ROOM-穿线房, EVENT-赛事 */
    private String targetRole;

    /** 优先级, 数值越大越优先 */
    private Integer priority;

    /** 状态: 1-启用, 0-停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
