package com.syty.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.common.BizException;
import com.syty.entity.Customer;
import com.syty.entity.PunchCard;
import com.syty.entity.PunchCardLog;
import com.syty.mapper.CustomerMapper;
import com.syty.mapper.PunchCardMapper;
import com.syty.mapper.PunchCardLogMapper;
import com.syty.dto.PunchCardIssueRequest;
import com.syty.dto.PunchCardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PunchCardService extends ServiceImpl<PunchCardMapper, PunchCard> {

    private final PunchCardLogMapper logMapper;
    private final CustomerMapper customerMapper;

    /**
     * 发卡逻辑 (V1.8 修正：关联 Customer)
     */
    @Transactional(rollbackFor = Exception.class)
    public void issueCard(PunchCardIssueRequest req) {
        // 1. 查找客户 (如果没有则创建)
        Customer customer = getOrCreateCustomer(req.getPhone(), req.getTenantId());

        // 2. 计算次数和过期时间
        int count = "TEN_TIMES".equals(req.getCardType()) ? 10 : 
                    "TWENTY_TIMES".equals(req.getCardType()) ? 20 : 0;
        
        PunchCard card = new PunchCard();
        card.setTenantId(req.getTenantId());
        card.setCustomerId(customer.getId()); // 关键修正：绑定 Customer ID
        card.setCardType(req.getCardType());
        card.setTotalCount(count);
        card.setRemainingCount(count);
        card.setUsedCount(0);
        card.setStatus(1);
        card.setExpireTime(LocalDateTime.now().plusDays(req.getValidDays() != null ? req.getValidDays() : 90));
        
        save(card);
        log.info("[次卡] 发卡成功，客户 {} -> 次数 {}", customer.getId(), count);
    }

    /**
     * 核心：扣次逻辑 (带乐观锁)
     */
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long cardId, Long orderId, Long tenantId, Long customerId) {
        // 1. 获取卡片
        PunchCard card = getById(cardId);
        if (card == null || !card.getTenantId().equals(tenantId)) {
            throw new BizException("次卡不存在或归属错误");
        }

        // 2. 校验有效性
        if (!card.isValid()) {
            String reason = card.getStatus() == 3 ? "卡片已过期" : "剩余次数不足";
            throw new BizException(reason);
        }

        // 2.5 越权防护：确保扣次卡片属于当前客户
        if (!card.getCustomerId().equals(customerId)) {
            throw new BizException("次卡归属客户与订单客户不一致");
        }

        // 3. 乐观锁扣次
        card.setRemainingCount(card.getRemainingCount() - 1);
        card.setUsedCount(card.getUsedCount() + 1);
        
        boolean success = updateById(card);
        if (!success) {
            throw new BizException("次卡扣次失败（并发冲突或数据异常），请重试");
        }

        // 4. 检查是否用完
        if (card.getRemainingCount() <= 0) {
            card.setStatus(2);
            updateById(card);
        }

        // 5. 记录流水
        PunchCardLog logEntry = new PunchCardLog();
        logEntry.setCardId(cardId);
        logEntry.setOrderId(orderId);
        logEntry.setCustomerId(customerId); // 记录客户 ID
        logEntry.setChangeCount(-1);
        logEntry.setRemainingAfter(card.getRemainingCount());
        logEntry.setReason("ORDER_COMPLETE");
        logMapper.insert(logEntry);
    }

    /**
     * 查询客户可用次卡
     */
    public List<PunchCardResponse> getAvailableCards(String phone, Long tenantId) {
        // 先找客户
        Customer customer = customerMapper.selectByPhone(phone);
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            return List.of();
        }

        // 查次卡
        List<PunchCard> cards = list(new LambdaQueryWrapper<PunchCard>()
                .eq(PunchCard::getCustomerId, customer.getId())
                .eq(PunchCard::getTenantId, tenantId)
                .eq(PunchCard::getStatus, 1)
                .orderByDesc(PunchCard::getCreatedAt));

        return cards.stream().map(c -> {
            PunchCardResponse resp = new PunchCardResponse();
            resp.setId(c.getId());
            resp.setCardType(c.getCardType());
            resp.setRemainingCount(c.getRemainingCount());
            resp.setExpireTime(c.getExpireTime());
            return resp;
        }).collect(Collectors.toList());
    }
    
    /**
     * 辅助方法：查找或创建客户
     */
    private Customer getOrCreateCustomer(String phone, Long tenantId) {
        Customer customer = customerMapper.selectByPhone(phone);
        if (customer == null) {
            customer = new Customer();
            customer.setPhone(phone);
            customer.setTenantId(tenantId);
            customer.setNickname("新会员");
            customer.setIsMember(0);
            customerMapper.insert(customer);
            log.info("[次卡] 自动注册客户: {}", phone);
        }
        return customer;
    }
}
