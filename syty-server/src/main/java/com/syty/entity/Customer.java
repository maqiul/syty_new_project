package com.syty.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 门店客户实体 (V1.8 核心修正：与 Player 隔离)
 */
@Data
@TableName("customer")
public class Customer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 手机号 (唯一身份凭证) */
    private String phone;

    /** 微信 OpenID (H5 登录用) */
    private String wechatOpenid;

    private String nickname;

    /** 是否会员：0-散客 1-会员 */
    private Integer isMember;

    private String memberLevel;

    /** 累计消费次数 */
    private Integer orderCount;

    /** 最后一次消费日期 */
    private LocalDate lastOrderDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
