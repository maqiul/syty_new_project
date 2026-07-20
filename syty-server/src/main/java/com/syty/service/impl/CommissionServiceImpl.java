package com.syty.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syty.common.BizException;
import com.syty.entity.CommissionRule;
import com.syty.entity.StringingOrder;
import com.syty.entity.SysUser;
import com.syty.mapper.CommissionRuleMapper;
import com.syty.mapper.StringingOrderMapper;
import com.syty.mapper.SysUserMapper;
import com.syty.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
/**
 * 提成计算 Service 实现
 *
 * 计算逻辑：
 * 1. 查询当前生效的提成规则（按优先级：个人规则 > 店铺规则 > 全局规则）
 * 2. 根据 ruleType 计算提成金额
 *    - FIXED: 使用 fixedAmount
 *    - PERCENT: 根据 percentBase 选择基数 × percentRate / 100
 *    - TIERED: 解析 tierConfig JSON，根据 actualAmount 匹配阶梯
 * 3. 更新订单 commission 字段
 * 4. 自动补充穿线师姓名（如有）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {
    private final CommissionRuleMapper commissionRuleMapper;
    private final StringingOrderMapper stringingOrderMapper;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateCommission(Long orderId) {
        // 1. 查询订单
        StringingOrder order = stringingOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在，orderId=" + orderId);
        }
        if (order.getActualAmount() == null && order.getTotalPrice() == null) {
            throw new BizException("订单金额为空，无法计算提成");
        }
        Long shopId = order.getShopId();
        Long stringerId = order.getStringerId();
        // 2. 查询生效的提成规则（优先级：个人 > 店铺 > 全局）
        CommissionRule rule = findEffectiveRule(order.getTenantId(), shopId, stringerId);
        if (rule == null) {
            log.warn("未找到有效的提成规则: tenantId={}, shopId={}, stringerId={}, orderId={}",
                    order.getTenantId(), shopId, stringerId, orderId);
            order.setCommission(BigDecimal.ZERO);
            stringingOrderMapper.updateById(order);
            return;
        }
        // 3. 计算提成
        BigDecimal commission = calculateByRule(rule, order);
        // 4. 自动补充穿线师姓名（如有）
        if (stringerId != null && (order.getStringerName() == null || order.getStringerName().isBlank())) {
            SysUser user = sysUserMapper.selectById(stringerId);
            if (user != null) {
                order.setStringerName(user.getRealName());
            }
        }
        // 5. 更新订单
        order.setCommission(commission);
        stringingOrderMapper.updateById(order);
        log.info("提成计算完成: orderId={}, ruleType={}, commission={}", orderId, rule.getRuleType(), commission);
    }
    /**
     * 查找生效的提成规则
     * 优先级：穿线师个人规则 > 店铺规则 > 全局规则
     */
    private CommissionRule findEffectiveRule(Long tenantId, Long shopId, Long stringerId) {
        LocalDate today = LocalDate.now();
        // 优先级1: 穿线师个人规则
        if (stringerId != null) {
            CommissionRule rule = getOneRule(tenantId, shopId, stringerId, today);
            if (rule != null) return rule;
            // 跨店铺通用个人规则
            rule = getOneRule(tenantId, null, stringerId, today);
            if (rule != null) return rule;
        }
        // 优先级2: 店铺默认规则
        if (shopId != null) {
            CommissionRule rule = getOneRule(tenantId, shopId, null, today);
            if (rule != null) return rule;
        }
        // 优先级3: 全局默认规则
        return getOneRule(tenantId, null, null, today);
    }
    private CommissionRule getOneRule(Long tenantId, Long shopId, Long stringerId, LocalDate today) {
        LambdaQueryWrapper<CommissionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommissionRule::getTenantId, tenantId);
        if (shopId != null) {
            wrapper.eq(CommissionRule::getShopId, shopId);
        } else {
            wrapper.isNull(CommissionRule::getShopId);
        }
        if (stringerId != null) {
            wrapper.eq(CommissionRule::getStringerId, stringerId);
        } else {
            wrapper.isNull(CommissionRule::getStringerId);
        }
        wrapper.eq(CommissionRule::getEnabled, 1);
        wrapper.le(CommissionRule::getEffectiveFrom, today);
        wrapper.and(w -> w.isNull(CommissionRule::getEffectiveTo)
                .or().ge(CommissionRule::getEffectiveTo, today));
        wrapper.last("LIMIT 1");
        return commissionRuleMapper.selectOne(wrapper);
    }
    /**
     * 根据规则类型计算提成金额
     */
    private BigDecimal calculateByRule(CommissionRule rule, StringingOrder order) {
        BigDecimal actualAmount = order.getActualAmount() != null ? order.getActualAmount() : order.getTotalPrice();
        BigDecimal laborPrice = order.getLaborPrice() != null ? order.getLaborPrice() : BigDecimal.ZERO;
        // 线材费用 = 主线价格 + 横线价格
        BigDecimal stringPrice = BigDecimal.ZERO;
        if (order.getMainPrice() != null) stringPrice = stringPrice.add(order.getMainPrice());
        if (order.getCrossPrice() != null) stringPrice = stringPrice.add(order.getCrossPrice());
        switch (rule.getRuleType()) {
            case "FIXED":
                if (rule.getFixedAmount() == null) {
                    throw new BizException("FIXED规则缺少 fixedAmount 配置");
                }
                return rule.getFixedAmount();
            case "PERCENT":
                if (rule.getPercentRate() == null) {
                    throw new BizException("PERCENT规则缺少 percentRate 配置");
                }
                BigDecimal base;
                String baseType = rule.getPercentBase();
                switch (baseType != null ? baseType : "LABOR") {
                    case "LABOR":
                        base = laborPrice;
                        break;
                    case "TOTAL":
                        base = actualAmount;
                        break;
                    case "STRING":
                        base = stringPrice;
                        break;
                    default:
                        base = laborPrice;
                }
                return base.multiply(rule.getPercentRate())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            case "TIERED":
                if (rule.getTierConfig() == null || rule.getTierConfig().isBlank()) {
                    throw new BizException("TIERED规则缺少 tierConfig 配置");
                }
                return calculateTiered(rule.getTierConfig(), actualAmount);
            default:
                throw new BizException("不支持的提成规则类型: " + rule.getRuleType());
        }
    }
    /**
     * 计算阶梯提成
     * tierConfig 格式示例:
     * [
     *   {"minAmount": 0, "maxAmount": 50, "commission": 5},
     *   {"minAmount": 50, "maxAmount": 100, "commission": 10},
     *   {"minAmount": 100, "maxAmount": null, "commission": 15}
     * ]
     */
    private BigDecimal calculateTiered(String tierConfigJson, BigDecimal actualAmount) {
        try {
            JsonNode tiers = objectMapper.readTree(tierConfigJson);
            if (!tiers.isArray() || tiers.isEmpty()) {
                throw new BizException("阶梯配置格式错误");
            }
            for (JsonNode tier : tiers) {
                BigDecimal minAmount = tier.has("minAmount") && !tier.get("minAmount").isNull()
                        ? tier.get("minAmount").decimalValue() : BigDecimal.ZERO;
                BigDecimal maxAmount = tier.has("maxAmount") && !tier.get("maxAmount").isNull()
                        ? tier.get("maxAmount").decimalValue() : null;
                BigDecimal commission = tier.has("commission") && !tier.get("commission").isNull()
                        ? tier.get("commission").decimalValue() : BigDecimal.ZERO;
                boolean match = actualAmount.compareTo(minAmount) >= 0;
                if (maxAmount != null) {
                    match = match && actualAmount.compareTo(maxAmount) < 0;
                }
                if (match) {
                    return commission;
                }
            }
            // 未匹配任何阶梯，返回0
            log.warn("阶梯规则未匹配到任何档位, actualAmount={}", actualAmount);
            return BigDecimal.ZERO;
        } catch (Exception e) {
            throw new BizException("阶梯提成计算失败: " + e.getMessage());
        }
    }
}
