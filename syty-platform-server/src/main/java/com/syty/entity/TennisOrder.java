package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * @deprecated V1.1 已合并到 {@link StringingOrder} 表，使用 sportType='TENNIS' 区分
 * 此表数据已迁移至 stringing_order 表，后续版本将删除此字
 */
@Deprecated
@Data
@TableName("tennis_order")
public class TennisOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String orderNo;
    private Long shopId;
    private Long playerId;
    private Long racketId;
    private Long mainStringId;
    private Long crossStringId;
    private String playerPhone;
    private BigDecimal mainTension;
    private BigDecimal crossTension;
    private BigDecimal mainPrice;
    private BigDecimal crossPrice;
    private BigDecimal laborPrice;
    private BigDecimal totalPrice;
    private String orderPhotoUrl;
    private Integer status;
    private String remark;
    private String source;
    private Integer printed;
    private Integer printCount;
    // 关联展示字段
    @TableField(exist = false)
    private String shopName;
    @TableField(exist = false)
    private String playerName;
    @TableField(exist = false)
    private String racketName;
    @TableField(exist = false)
    private String mainStringName;
    @TableField(exist = false)
    private String crossStringName;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
