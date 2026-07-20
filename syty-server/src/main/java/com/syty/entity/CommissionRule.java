package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * 提成规则配置表
 */
@Data
@TableName("commission_rule")
public class CommissionRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long shopId;        // NULL=全局规则
    private Long stringerId;    // NULL=默认规则
    // 规则类型
    private String ruleType;    // FIXED-固定金额 PERCENT-百分比 TIERED-阶梯
    // 固定金额规则
    private BigDecimal fixedAmount;  // 固定提成金额(元)
    // 百分比规则
    private BigDecimal percentRate;  // 提成比例(%, 如15.00 表示15%)
    private String percentBase;      // 提成基数: LABOR-手工费 TOTAL-总价 STRING-线材费
    // 阶梯规则 (JSON)
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String tierConfig;       // 阶梯配置JSON
    // 生效时间
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;   // NULL=永久有效
    private Integer enabled;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
