package com.syty.service.impl;

import com.syty.common.BizException;
import com.syty.common.TenantContext;
import com.syty.entity.ShopString;
import com.syty.entity.StockLog;
import com.syty.mapper.ShopStringMapper;
import com.syty.mapper.StockLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StockService 单元测试
 * 测试库存管理的核心逻辑：预留、扣减、释放
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("库存服务测试")
class StockServiceTest {

    @Mock
    private ShopStringMapper shopStringMapper;

    @Mock
    private StockLogMapper stockLogMapper;

    @InjectMocks
    private StockServiceImpl stockService;

    private ShopString testShopString;

    @BeforeEach
    void setUp() {
        testShopString = new ShopString();
        testShopString.setId(1L);
        testShopString.setShopId(1L);
        testShopString.setStringId(10L);
        testShopString.setStock(100);
        testShopString.setReservedQuantity(10);
    }

    @Test
    @DisplayName("预留库存 - 正常流程")
    void reserveStock_Success() {
        // Given
        when(shopStringMapper.selectForUpdate(1L, 10L)).thenReturn(testShopString);
        when(shopStringMapper.updateReservedStock(1L, 5)).thenReturn(1);
        when(stockLogMapper.insert(any(StockLog.class))).thenReturn(1);

        try (MockedStatic<TenantContext> mockedContext = mockStatic(TenantContext.class)) {
            mockedContext.when(TenantContext::getTenantId).thenReturn(1L);
            mockedContext.when(TenantContext::getUserId).thenReturn(1L);
            mockedContext.when(TenantContext::getUsername).thenReturn("test_user");

            // When
            stockService.reserveStock(1L, 10L, 5, 100L, "SO001");

            // Then
            verify(shopStringMapper).updateReservedStock(1L, 5);
            verify(stockLogMapper).insert(argThat(log -> 
                "RESERVE".equals(log.getChangeType()) && 
                log.getQuantity() == 5 &&
                log.getOrderId().equals(100L)
            ));
        }
    }

    @Test
    @DisplayName("预留库存 - 数量<=0 抛出异常")
    void reserveStock_InvalidQuantity_ThrowsException() {
        // When & Then
        BizException exception = assertThrows(BizException.class, () -> {
            stockService.reserveStock(1L, 10L, 0, 100L, "SO001");
        });
        assertTrue(exception.getMessage().contains("预留数量必须大于0"));
    }

    @Test
    @DisplayName("预留库存 - 线材不存在抛出异常")
    void reserveStock_StringNotFound_ThrowsException() {
        // Given
        when(shopStringMapper.selectForUpdate(1L, 10L)).thenReturn(null);

        // When & Then
        BizException exception = assertThrows(BizException.class, () -> {
            stockService.reserveStock(1L, 10L, 5, 100L, "SO001");
        });
        assertTrue(exception.getMessage().contains("店铺未配置该线材库存"));
    }

    @Test
    @DisplayName("预留库存 - 可用库存不足抛出异常")
    void reserveStock_InsufficientAvailableStock_ThrowsException() {
        // Given - 总库存100，已预留10，可用90，但需求100
        testShopString.setStock(100);
        testShopString.setReservedQuantity(10);
        when(shopStringMapper.selectForUpdate(1L, 10L)).thenReturn(testShopString);

        try (MockedStatic<TenantContext> mockedContext = mockStatic(TenantContext.class)) {
            mockedContext.when(TenantContext::getTenantId).thenReturn(1L);

            // When & Then
            BizException exception = assertThrows(BizException.class, () -> {
                stockService.reserveStock(1L, 10L, 100, 100L, "SO001");
            });
            assertTrue(exception.getMessage().contains("可用库存不足"));
        }
    }

    @Test
    @DisplayName("扣减库存 - 正常流程")
    void deductStock_Success() {
        // Given
        when(shopStringMapper.selectForUpdate(1L, 10L)).thenReturn(testShopString);
        when(shopStringMapper.updateStockWithCheck(1L, -5, 1L)).thenReturn(1);
        when(shopStringMapper.updateReservedStock(1L, -5)).thenReturn(1);
        when(stockLogMapper.insert(any(StockLog.class))).thenReturn(1);

        try (MockedStatic<TenantContext> mockedContext = mockStatic(TenantContext.class)) {
            mockedContext.when(TenantContext::getTenantId).thenReturn(1L);
            mockedContext.when(TenantContext::getUserId).thenReturn(1L);
            mockedContext.when(TenantContext::getUsername).thenReturn("test_user");

            // When
            stockService.deductStock(1L, 10L, 5, 100L, "SO001");

            // Then
            verify(shopStringMapper).updateStockWithCheck(1L, -5, 1L);
            verify(shopStringMapper).updateReservedStock(1L, -5);
            verify(stockLogMapper).insert(argThat(log -> 
                "ORDER_OUT".equals(log.getChangeType()) && 
                log.getQuantity() == 5
            ));
        }
    }

    @Test
    @DisplayName("扣减库存 - 数量<=0 抛出异常")
    void deductStock_InvalidQuantity_ThrowsException() {
        // When & Then
        BizException exception = assertThrows(BizException.class, () -> {
            stockService.deductStock(1L, 10L, 0, 100L, "SO001");
        });
        assertTrue(exception.getMessage().contains("扣减数量必须大于0"));
    }

    @Test
    @DisplayName("扣减库存 - 总库存不足抛出异常")
    void deductStock_InsufficientTotalStock_ThrowsException() {
        // Given - 总库存只有3，但需求5
        testShopString.setStock(3);
        when(shopStringMapper.selectForUpdate(1L, 10L)).thenReturn(testShopString);

        try (MockedStatic<TenantContext> mockedContext = mockStatic(TenantContext.class)) {
            mockedContext.when(TenantContext::getTenantId).thenReturn(1L);

            // When & Then
            BizException exception = assertThrows(BizException.class, () -> {
                stockService.deductStock(1L, 10L, 5, 100L, "SO001");
            });
            assertTrue(exception.getMessage().contains("库存不足"));
        }
    }

    @Test
    @DisplayName("释放库存 - 正常流程")
    void releaseStock_Success() {
        // Given
        when(shopStringMapper.selectForUpdate(1L, 10L)).thenReturn(testShopString);
        when(shopStringMapper.updateReservedStock(1L, -3)).thenReturn(1);
        when(stockLogMapper.insert(any(StockLog.class))).thenReturn(1);

        try (MockedStatic<TenantContext> mockedContext = mockStatic(TenantContext.class)) {
            mockedContext.when(TenantContext::getTenantId).thenReturn(1L);
            mockedContext.when(TenantContext::getUserId).thenReturn(1L);
            mockedContext.when(TenantContext::getUsername).thenReturn("test_user");

            // When
            stockService.releaseStock(1L, 10L, 3, 100L, "SO001");

            // Then
            verify(shopStringMapper).updateReservedStock(1L, -3);
            verify(stockLogMapper).insert(argThat(log -> 
                "RELEASE".equals(log.getChangeType()) && 
                log.getQuantity() == -3  // 负数表示回补
            ));
        }
    }

    @Test
    @DisplayName("释放库存 - 预留库存不足抛出异常")
    void releaseStock_InsufficientReservedStock_ThrowsException() {
        // Given - 已预留5，但需求释放10
        testShopString.setReservedQuantity(5);
        when(shopStringMapper.selectForUpdate(1L, 10L)).thenReturn(testShopString);

        try (MockedStatic<TenantContext> mockedContext = mockStatic(TenantContext.class)) {
            mockedContext.when(TenantContext::getTenantId).thenReturn(1L);

            // When & Then
            BizException exception = assertThrows(BizException.class, () -> {
                stockService.releaseStock(1L, 10L, 10, 100L, "SO001");
            });
            assertTrue(exception.getMessage().contains("预留库存不足"));
        }
    }

    @Test
    @DisplayName("库存流水记录 - 包含完整信息")
    void writeLog_ContainsCompleteInfo() {
        // Given
        when(shopStringMapper.selectForUpdate(1L, 10L)).thenReturn(testShopString);
        when(shopStringMapper.updateReservedStock(1L, 5)).thenReturn(1);
        when(stockLogMapper.insert(any(StockLog.class))).thenReturn(1);

        try (MockedStatic<TenantContext> mockedContext = mockStatic(TenantContext.class)) {
            mockedContext.when(TenantContext::getTenantId).thenReturn(1L);
            mockedContext.when(TenantContext::getUserId).thenReturn(100L);
            mockedContext.when(TenantContext::getUsername).thenReturn("admin");

            // When
            stockService.reserveStock(1L, 10L, 5, 200L, "SO002");

            // Then
            verify(stockLogMapper).insert(argThat(log -> {
                assertEquals(1L, log.getTenantId());
                assertEquals(1L, log.getShopId());
                assertEquals(10L, log.getStringId());
                assertEquals("RESERVE", log.getChangeType());
                assertEquals(5, log.getQuantity());
                assertEquals(200L, log.getOrderId());
                assertEquals("SO002", log.getOrderNo());
                assertEquals(100L, log.getOperatorId());
                assertEquals("admin", log.getOperatorName());
                assertEquals("接单预留", log.getRemark());
                return true;
            }));
        }
    }
}
