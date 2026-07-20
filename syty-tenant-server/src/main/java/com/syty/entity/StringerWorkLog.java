package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stringer_work_log")
public class StringerWorkLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long orderId;
    private Long stringerId;
    private String stringerName;
    private BigDecimal fee;
    private Integer duration; // minutes
    private LocalDateTime workTime;
}
