package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("player")
public class Player {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String phone;
    private Integer gender;
    // === V1.1 新增字段 ===
    private String sportType;    // 运动类型: BADMINTON-羽毛球 TENNIS-网球 DUAL-双修
    private String ranking;      // 网球排名（仅网球有效）
    private Long tenantId;       // 所属租户
    private String remark;
    private String country;      // 国家/地区
    private String photoUrl;     // 球员照片
    private String skillLevel;   // 水平等级
    private String progressNotes; // 近期进步
    @TableField(exist = false)
    private String defaultRacketBrand;  // 默认球拍品牌
    @TableField(exist = false)
    private String defaultRacketModel;  // 默认球拍型号
    @TableField(exist = false)
    private String defaultTension;      // 默认磅数
    @TableField(exist = false)
    private String defaultStringType;   // 默认线型
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
