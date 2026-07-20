-- V1.5 数据库迁移: 模板/策略按店铺隔离
-- 执行日期: 2026-05-08
-- 说明: 为 sys_print_template 和 sys_print_policy 增加 shop_id 字段
-- 执行人: 老李(DBA)

-- ========================================
-- 1. sys_print_template 增加 shop_id 字段
-- ========================================
ALTER TABLE sys_print_template 
ADD COLUMN shop_id BIGINT DEFAULT NULL COMMENT '店铺ID, NULL=公共模板, 非NULL=店铺专属模板' 
AFTER name;

-- 将现有的 GLOBAL 模板标记为公共模板 (shop_id = NULL)
UPDATE sys_print_template SET shop_id = NULL WHERE scope = 'GLOBAL';

-- 添加索引: 加速店铺模板查询
CREATE INDEX idx_template_shop_id ON sys_print_template(shop_id);

-- ========================================
-- 2. sys_print_policy 增加 shop_id 字段
--    (如果已有则跳过, 执行前请确认)
-- ========================================
-- 先检查字段是否已存在 (MySQL 8.0+ 写法)
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'sys_print_policy' 
    AND COLUMN_NAME = 'shop_id'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_print_policy ADD COLUMN shop_id BIGINT DEFAULT NULL COMMENT ''店铺ID, NULL=公共策略, 非NULL=店铺专属策略'' AFTER id',
    'SELECT ''shop_id 字段已存在, 跳过'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为已有策略添加店铺维度索引
CREATE INDEX idx_policy_shop_id ON sys_print_policy(shop_id);

-- ========================================
-- 3. 验证
-- ========================================
-- 检查模板表结构
-- DESCRIBE sys_print_template;

-- 检查公共模板数量
-- SELECT COUNT(*) AS public_template_count FROM sys_print_template WHERE shop_id IS NULL;

-- 检查公共策略数量
-- SELECT COUNT(*) AS public_policy_count FROM sys_print_policy WHERE shop_id IS NULL;
