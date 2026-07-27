package com.syty.service.impl;

import com.syty.common.BizException;
import com.syty.entity.StringingOrder;
import com.syty.mapper.StringingOrderMapper;
import com.syty.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StringingOrderService 单元测试
 * 测试订单完成流程的核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单服务测试")
class StringingOrderServiceTest {

    @Mock
    private StringingOrderMapper orderMapper;

    @Mock
    private StockService stockService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private CommissionService commissionService;

    @Mock
    private PunchCardService punchCardService;

    @InjectMocks
    private StringingOrderServiceImpl orderService;

    private StringingOrder testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new StringingOrder();
        testOrder.setId(1L);
        testOrder.setOrderNo("SO20260727001");
        testOrder.setShopId(1L);
        testOrder.setPlayerId(1L);
        testOrder.setMainStringId(10L);
        testOrder.setCrossStringId(20L);
        testOrder.setStatus(0); // 待处理
        testOrder.setTenantId(1L);
    }

    @Test
    @DisplayName("完成订单 - 正常流程（扣库存+记录支付+计算提成）")
    void completeOrder_Success() {
        // Given
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(orderMapper.updateById(any())).thenReturn(1);

        // When
        orderService.completeOrder(1L, new BigDecimal("100.00"), "WECHAT");

        // Then
        verify(stockService, times(2)).deductStock(eq(1L), anyLong(), eq(1), eq(1L), eq("SO20260727001"));
        verify(paymentService).recordPayment(1L, new BigDecimal("100.00"), "WECHAT");
        verify(commissionService).calculateCommission(1L);
        
        // 验证订单状态更新为已完成（status=2）
        verify(orderMapper).updateById(argThat(order -> 
            order.getId().equals(1L) && order.getStatus() == 2
        ));
    }

    @Test
    @DisplayName("完成订单 - 订单不存在抛出异常")
    void completeOrder_OrderNotFound_ThrowsException() {
        // Given
        when(orderMapper.selectById(999L)).thenReturn(null);

        // When & Then
        BizException exception = assertThrows(BizException.class, () -> {
            orderService.completeOrder(999L, BigDecimal.ZERO, "CASH");
        });
        assertTrue(exception.getMessage().contains("订单不存在"));
    }

    @Test
    @DisplayName("完成订单 - 订单状态不允许完成（已完成）")
    void completeOrder_InvalidStatus_ThrowsException() {
        // Given
        testOrder.setStatus(2); // 已完成
        when(orderMapper.selectById(1L)).thenReturn(testOrder);

        // When & Then
        BizException exception = assertThrows(BizException.class, () -> {
            orderService.completeOrder(1L, BigDecimal.ZERO, "CASH");
        });
        assertTrue(exception.getMessage().contains("订单状态不允许完成"));
    }

    @Test
    @DisplayName("完成订单 - 无主线库存时不扣减")
    void completeOrder_NoMainString_SkipDeduct() {
        // Given
        testOrder.setMainStringId(null);
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(orderMapper.updateById(any())).thenReturn(1);

        // When
        orderService.completeOrder(1L, null, null);

        // Then - 只扣减横线库存
        verify(stockService, times(1)).deductStock(eq(1L), eq(20L), eq(1), eq(1L), anyString());
        verify(stockService, never()).deductStock(eq(1L), eq(10L), anyInt(), anyLong(), anyString());
    }

    @Test
    @DisplayName("完成订单 - 无横线库存时不扣减")
    void completeOrder_NoCrossString_SkipDeduct() {
        // Given
        testOrder.setCrossStringId(null);
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(orderMapper.updateById(any())).thenReturn(1);

        // When
        orderService.completeOrder(1L, null, null);

        // Then - 只扣减主线库存
        verify(stockService, times(1)).deductStock(eq(1L), eq(10L), eq(1), eq(1L), anyString());
        verify(stockService, never()).deductStock(eq(1L), eq(20L), anyInt(), anyLong(), anyString());
    }

    @Test
    @DisplayName("完成订单 - 无支付信息时不记录支付")
    void completeOrder_NoPayment_SkipRecord() {
        // Given
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(orderMapper.updateById(any())).thenReturn(1);

        // When
        orderService.completeOrder(1L, null, null);

        // Then
        verify(paymentService, never()).recordPayment(anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("完成订单 - 使用次卡时扣减次卡")
    void completeOrder_WithPunchCard_DeductPunchCard() {
        // Given
        testOrder.setUsePunchCard(true);
        testOrder.setPunchCardId(100L);
        testOrder.setCustomerId(50L);
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(orderMapper.updateById(any())).thenReturn(1);

        // When
        orderService.completeOrder(1L, BigDecimal.ZERO, "PUNCH_CARD");

        // Then
        verify(punchCardService).deduct(100L, 1L, 1L, 50L);
    }

    @Test
    @DisplayName("完成订单 - 次卡扣减失败不影响订单完成")
    void completeOrder_PunchCardDeductFailed_OrderStillComplete() {
        // Given
        testOrder.setUsePunchCard(true);
        testOrder.setPunchCardId(100L);
        testOrder.setCustomerId(50L);
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(orderMapper.updateById(any())).thenReturn(1);
        doThrow(new BizException("次卡余额不足")).when(punchCardService).deduct(anyLong(), anyLong(), anyLong(), anyLong());

        // When - 不应该抛出异常
        assertDoesNotThrow(() -> orderService.completeOrder(1L, BigDecimal.ZERO, "PUNCH_CARD"));

        // Then - 订单仍然完成
        verify(orderMapper).updateById(argThat(order -> order.getStatus() == 2));
    }
}
