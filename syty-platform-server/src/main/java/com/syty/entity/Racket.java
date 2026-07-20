package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("racket")
public class Racket {
    @TableId(type = IdType.AUTO)
    private Long id;
    // === V1.1 新增字段 ===
    private String sportType;   // 运动类型: BADMINTON-羽毛字TENNIS-网球
    private Long tenantId;      // 所属租字
    private String brand;
    private String model;
    @TableField("spec")
    private String specs;       // 羽毛球规字字4U/3U/G5)
    // 网球专属字段
    private String headSize;    // 拍面大小(平方英寸)
    private String stringPattern; // 线床规格(字16x19)
    private String weight;      // 空拍重量(字
    private String balance;     // 平衡字mm)
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
