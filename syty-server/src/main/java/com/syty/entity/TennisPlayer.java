package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * @deprecated V1.1 已合并到 {@link Player} 表，使用 sportType='TENNIS' 区分
 * 此表数据已迁移至 player 表，后续版本将删除此字
 */
@Deprecated
@Data
@TableName("tennis_player")
public class TennisPlayer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String phone;
    private Integer gender;
    private String ranking;
    private String photoUrl;
    private String skillLevel;
    private String progressNotes;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
