-- V1.8 核心架构重塑：赛事与门店彻底分离
-- 1. 建立门店客户表 (Customer) - 独立于赛事 Player

CREATE TABLE IF NOT EXISTS "customer" (
    "id"                BIGSERIAL        NOT NULL PRIMARY KEY,
    "tenant_id"         BIGINT           NOT NULL,
    
    -- 身份标识
    "phone"             VARCHAR(20)      UNIQUE,
    "wechat_openid"     VARCHAR(64)      UNIQUE,
    "nickname"          VARCHAR(64),
    
    -- 会员状态
    "is_member"         SMALLINT         DEFAULT 0,  -- 0:散客 1:会员
    "member_level"      VARCHAR(32),
    
    -- 统计
    "order_count"       INT              DEFAULT 0,
    "last_order_date"   DATE,

    "created_at"        TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

-- 2. 改造穿线工单：增加 customer_id，保留 player_id 做关联
-- 注意：如果字段已存在会报错，请手动检查
ALTER TABLE "stringing_order" 
    ADD COLUMN IF NOT EXISTS "customer_id" BIGINT;

-- 3. 次卡表重构 (删除旧表重建，确保外键正确指向 customer)
-- 警告：此操作会清空次卡测试数据
DROP TABLE IF EXISTS "punch_card_log" CASCADE;
DROP TABLE IF EXISTS "punch_card" CASCADE;

CREATE TABLE "punch_card" (
    "id"                BIGSERIAL        NOT NULL PRIMARY KEY,
    "customer_id"       BIGINT           NOT NULL,  -- 修正：绑定 Customer
    "tenant_id"         BIGINT           NOT NULL,
    "card_type"         VARCHAR(32)      NOT NULL,
    "total_count"       INT              DEFAULT 0,
    "remaining_count"   INT              DEFAULT 0,
    "status"            SMALLINT         DEFAULT 1, -- 1:正常 2:用完 3:过期
    "version"           INT              DEFAULT 0, -- 乐观锁
    "expire_time"       TIMESTAMP,
    "created_at"        TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "punch_card_log" (
    "id"                BIGSERIAL        NOT NULL PRIMARY KEY,
    "customer_id"       BIGINT           NOT NULL,
    "card_id"           BIGINT           NOT NULL,
    "order_id"          BIGINT           NOT NULL,
    "change_count"      INT              NOT NULL,
    "remaining_after"   INT              NOT NULL,
    "reason"            VARCHAR(64),
    "created_at"        TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);
