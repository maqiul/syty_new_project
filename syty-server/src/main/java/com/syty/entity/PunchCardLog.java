package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 次卡扣次流水表
 */
@Data
@TableName("punch_card_log")
public class PunchCardLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联客户 */
    private Long customerId;

    private Long cardId;

    private Long orderId;

    private Integer changeCount;

    private Integer remainingAfter;

    private String reason;

    private LocalDateTime createdAt;
}
