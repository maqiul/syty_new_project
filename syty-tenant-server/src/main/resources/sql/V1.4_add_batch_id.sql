-- V1.4: 移动端自助登记 — 增加 batch_id 字段
-- 用于关联同一批提交的多个订单

ALTER TABLE `stringing_order`
    ADD COLUMN `batch_id` VARCHAR(64) DEFAULT NULL COMMENT '批次ID (UUID, 同一次移动端提交共享)'
    AFTER `order_no`;

-- 可选: 给 batch_id 加索引, 方便按批次查询
ALTER TABLE `stringing_order`
    ADD INDEX `idx_batch_id` (`batch_id`);
