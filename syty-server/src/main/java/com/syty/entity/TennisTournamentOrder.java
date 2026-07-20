package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("tennis_tournament_order")
public class TennisTournamentOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String orderNo;
    private Long shopId;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long playerId;
    private String playerName;
    private Long tournamentId;
    private String racketBrand;
    private String racketModel;
    private BigDecimal tension;
    private String stringType;
    private String orderPhotoUrl;
    private String remark;
    private Integer printed;
    // 比赛信息
    private String matchName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime matchDate;
    private String matchRound;
    private String opponentName;
    private String matchResult;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
