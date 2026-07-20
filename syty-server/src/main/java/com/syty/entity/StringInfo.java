package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("string_info")
public class StringInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String brand;
    private String model;
    private String color;
    @TableField("diameter")
    private BigDecimal gauge;
    @TableField(exist = false)
    private String type;
    // === V1.1 新增字段 ===
    private String sportType;   // 运动类型: BADMINTON-羽毛字TENNIS-网球
    private Long tenantId;      // 所属租字
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
