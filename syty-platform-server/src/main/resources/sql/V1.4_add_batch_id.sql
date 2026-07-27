-- V1.4: 移动端自助登记 — 增加 batch_id 字段
-- 用于关联同一批提交的多个订单

ALTER TABLE stringing_order
    ADD COLUMN IF NOT EXISTS batch_id VARCHAR(64) DEFAULT NULL;

COMMENT ON COLUMN stringing_order.batch_id IS '批次ID (UUID, 同一次移动端提交共享)';

CREATE INDEX IF NOT EXISTS idx_batch_id ON stringing_order(batch_id);
