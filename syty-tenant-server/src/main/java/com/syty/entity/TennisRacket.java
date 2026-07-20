package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * @deprecated V1.1 已合并到 {@link Racket} 表，使用 sportType='TENNIS' 区分
 * 此表数据已迁移至 racket 表，后续版本将删除此字
 */
@Deprecated
@Data
@TableName("tennis_racket")
public class TennisRacket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String brand;
    private String model;
    private String headSize;
    private String stringPattern;
    private String weight;
    @TableField(exist = false)
    private String balance;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
