package com.syty.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.entity.StringingOrder;
import com.syty.service.StockService;
import com.syty.service.StringingOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存预留释放定时任务 (V1.3.1)
 * 每天凌晨 2 点执行，释放超过 48 小时未完成订单的预留库存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockReleaseJob {

    private final StringingOrderService orderService;
    private final StockService stockService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void releaseHangingStock() {
        log.info("开始执行库存悬挂释放任务...");
        // 查询 48 小时前创建的、状态仍为 PENDING (0) 的订单
        LocalDateTime deadline = LocalDateTime.now().minusHours(48);
        LambdaQueryWrapper<StringingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringingOrder::getStatus, 0)
               .lt(StringingOrder::getCreatedAt, deadline)
               .eq(StringingOrder::getDeleted, 0);

        List<StringingOrder> expiredOrders = orderService.list(wrapper);
        
        for (StringingOrder order : expiredOrders) {
            try {
                log.info("检测到超时未接单: orderNo={}, 创建于: {}", order.getOrderNo(), order.getCreatedAt());
                
                // 1. 释放主线预留
                if (order.getMainStringId() != null) {
                    stockService.releaseStock(order.getShopId(), order.getMainStringId(), 1, order.getId(), order.getOrderNo());
                }
                // 2. 释放横线预留
                if (order.getCrossStringId() != null) {
                    stockService.releaseStock(order.getShopId(), order.getCrossStringId(), 1, order.getId(), order.getOrderNo());
                }
                
                // 3. 更新订单状态为已取消 (-1) 并记录原因
                order.setStatus(-1);
                order.setRemark(order.getRemark() + " [系统自动取消：超时 48 小时未接单，已释放库存]");
                orderService.updateById(order);
                
                log.info("成功释放超时订单库存: {}", order.getOrderNo());
            } catch (Exception e) {
                log.error("释放超时订单库存失败: orderNo={}", order.getOrderNo(), e);
            }
        }
        log.info("库存悬挂释放任务执行完毕，共处理 {} 个订单。", expiredOrders.size());
    }
}
