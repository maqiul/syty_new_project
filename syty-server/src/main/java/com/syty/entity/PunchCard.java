package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 电子次卡主表 (V1.8 修正：关联 Customer)
 */
@Data
@TableName("punch_card")
public class PunchCard implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联门店客户 */
    private Long customerId;

    private Long tenantId;

    /** 次卡类型: TEN_TIMES, TWENTY_TIMES */
    private String cardType;

    /** 总次数 */
    private Integer totalCount;

    /** 剩余次数 */
    private Integer remainingCount;

    /** 已用次数 */
    private Integer usedCount;

    /** 状态：1=正常，2=用完，3=过期，4=退款 */
    private Integer status;

    /** 乐观锁 */
    @Version
    private Integer version;

    /** 过期时间 */
    private LocalDateTime expireTime;

    private LocalDateTime createdAt;

    // --- 业务方法 ---
    public boolean isValid() {
        return this.status == 1 && this.remainingCount > 0 && this.expireTime.isAfter(LocalDateTime.now());
    }
}
