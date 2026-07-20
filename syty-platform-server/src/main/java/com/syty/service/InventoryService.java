package com.syty.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.dto.StockReservationResult;
import com.syty.entity.InventoryEntity;
import com.syty.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * 库存服务
 * 支持库存预留 (reserved_stock) 机制:
 * - 接单时: 预扣减 reserved_stock (乐观锁)
 * - 完成时: 实扣减 totalStock, 同时释放 reserved_stock
 * - 取消时: 释放 reserved_stock
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryMapper inventoryMapper;
    /**
     * 接单预留库存 (预扣减)
     * 使用数据库级别的乐观锁防止超卖
     *
     * @param skuCode 商品编号
     * @param amount  预留数量
     * @return 预留结果
     */
    @Transactional(rollbackFor = Exception.class)
    public StockReservationResult reserveStock(String skuCode, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("预留数量必须大于0");
        }
        log.info("开始预留库存: skuCode={}, amount={}", skuCode, amount);
        // Step 1: 查询当前库存 (行级锁: for update)
        LambdaQueryWrapper<InventoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryEntity::getSkuCode, skuCode)
               .eq(InventoryEntity::getStatus, 1);
        InventoryEntity inventory = inventoryMapper.selectOne(wrapper);
        if (inventory == null) {
            return StockReservationResult.builder()
                    .success(false)
                    .message("商品不存在或已停用: " + skuCode)
                    .build();
        }
        // 计算可用库存 = totalStock - reservedStock
        int available = inventory.getTotalStock() - inventory.getReservedStock();
        if (available < amount) {
            log.warn("库存不足: skuCode={}, available={}, required={}", skuCode, available, amount);
            return StockReservationResult.builder()
                    .success(false)
                    .availableBefore(available)
                    .availableAfter(available)
                    .reservedAmount(0)
                    .message(String.format("库存不足: 可用%d, 需要%d", available, amount))
                    .build();
        }
        // Step 2: 预扣减 (增加 reservedStock)
        inventory.setReservedStock(inventory.getReservedStock() + amount);
        int rows = inventoryMapper.updateById(inventory);
        if (rows <= 0) {
            throw new RuntimeException("库存预留失败: 数据库更新失败");
        }
        int newAvailable = inventory.getTotalStock() - inventory.getReservedStock();
        log.info("库存预留成功: skuCode={}, 预留{}件, 可用库存: {} -> {}",
                skuCode, amount, available, newAvailable);
        return StockReservationResult.builder()
                .success(true)
                .availableBefore(available)
                .availableAfter(newAvailable)
                .reservedAmount(amount)
                .message("库存预留成功")
                .build();
    }
    /**
     * 完成订单 - 实扣减库存
     * 从 totalStock 中扣减, 同时释放 reservedStock
     *
     * @param skuCode 商品编号
     * @param amount  扣减数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(String skuCode, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("扣减数量必须大于0");
        }
        log.info("开始实扣减库存: skuCode={}, amount={}", skuCode, amount);
        LambdaQueryWrapper<InventoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryEntity::getSkuCode, skuCode)
               .eq(InventoryEntity::getStatus, 1);
        InventoryEntity inventory = inventoryMapper.selectOne(wrapper);
        if (inventory == null) {
            throw new RuntimeException("商品不存在: " + skuCode);
        }
        if (inventory.getReservedStock() < amount) {
            throw new RuntimeException(
                    String.format("预留库存不足, 无法实扣: reserved=%d, required=%d",
                            inventory.getReservedStock(), amount));
        }
        // 实扣: totalStock 减少, reservedStock 同步减少
        inventory.setTotalStock(inventory.getTotalStock() - amount);
        inventory.setReservedStock(inventory.getReservedStock() - amount);
        int rows = inventoryMapper.updateById(inventory);
        if (rows <= 0) {
            throw new RuntimeException("库存实扣失败: 数据库更新失败");
        }
        log.info("库存实扣成功: skuCode={}, 扣减{}件, 剩余totalStock={}",
                skuCode, amount, inventory.getTotalStock());
    }
    /**
     * 取消订单 - 释放预留库存
     *
     * @param skuCode 商品编号
     * @param amount  释放数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseReservedStock(String skuCode, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("释放数量必须大于0");
        }
        log.info("开始释放预留库存: skuCode={}, amount={}", skuCode, amount);
        LambdaQueryWrapper<InventoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryEntity::getSkuCode, skuCode)
               .eq(InventoryEntity::getStatus, 1);
        InventoryEntity inventory = inventoryMapper.selectOne(wrapper);
        if (inventory == null) {
            throw new RuntimeException("商品不存在: " + skuCode);
        }
        if (inventory.getReservedStock() < amount) {
            throw new RuntimeException(
                    String.format("预留库存不足, 无法释放: reserved=%d, required=%d",
                            inventory.getReservedStock(), amount));
        }
        // 仅释放 reservedStock, 不扣 totalStock
        inventory.setReservedStock(inventory.getReservedStock() - amount);
        int rows = inventoryMapper.updateById(inventory);
        if (rows <= 0) {
            throw new RuntimeException("释放预留库存失败: 数据库更新失败");
        }
        log.info("预留库存释放成功: skuCode={}, 释放{}件, 当前available={}",
                skuCode, amount, inventory.getTotalStock() - inventory.getReservedStock());
    }
    /**
     * 查询可用库存
     */
    public int getAvailableStock(String skuCode) {
        LambdaQueryWrapper<InventoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryEntity::getSkuCode, skuCode)
               .eq(InventoryEntity::getStatus, 1);
        InventoryEntity inventory = inventoryMapper.selectOne(wrapper);
        if (inventory == null) {
            return 0;
        }
        return inventory.getTotalStock() - inventory.getReservedStock();
    }
}
