-- ==========================================
-- V1.5: 电子次卡表结构 (Punch Card) - PostgreSQL
-- ==========================================

-- 1. 次卡主表
CREATE TABLE IF NOT EXISTS "punch_card" (
    "id"                BIGSERIAL        NOT NULL PRIMARY KEY,
    "tenant_id"         BIGINT           NOT NULL,
    "member_id"         BIGINT           NOT NULL,
    "card_type"         VARCHAR(32)      NOT NULL,
    "total_count"       INT              NOT NULL DEFAULT 0,
    "remaining_count"   INT              NOT NULL DEFAULT 0,
    "used_count"        INT              NOT NULL DEFAULT 0,
    "status"            SMALLINT         NOT NULL DEFAULT 1,
    "version"           INT              NOT NULL DEFAULT 0,
    "expire_time"       TIMESTAMP        NOT NULL,
    "create_time"       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time"       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_punch_card_member_tenant" ON "punch_card" ("member_id", "tenant_id");

COMMENT ON TABLE "punch_card" IS '电子次卡主表';
COMMENT ON COLUMN "punch_card"."card_type" IS '次卡类型：TEN_TIMES, TWENTY_TIMES';
COMMENT ON COLUMN "punch_card"."status" IS '状态：1=正常，2=用完，3=过期，4=退款';
COMMENT ON COLUMN "punch_card"."version" IS '乐观锁版本号';

-- 2. 次卡流水表
CREATE TABLE IF NOT EXISTS "punch_card_log" (
    "id"                BIGSERIAL        NOT NULL PRIMARY KEY,
    "tenant_id"         BIGINT           NOT NULL,
    "card_id"           BIGINT           NOT NULL,
    "member_id"         BIGINT           NOT NULL,
    "order_id"          BIGINT           NOT NULL,
    "change_count"      INT              NOT NULL,
    "remaining_after"   INT              NOT NULL,
    "reason"            VARCHAR(64)      NOT NULL,
    "operator_id"       BIGINT,
    "remark"            VARCHAR(256),
    "create_time"       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_punch_card_log_card" ON "punch_card_log" ("card_id");
CREATE INDEX IF NOT EXISTS "idx_punch_card_log_order" ON "punch_card_log" ("order_id");

COMMENT ON TABLE "punch_card_log" IS '次卡扣次流水表';
COMMENT ON COLUMN "punch_card_log"."change_count" IS '变动次数 (-1 为扣次)';
COMMENT ON COLUMN "punch_card_log"."remaining_after" IS '变动后剩余次数';

-- 3. 修改 stringing_order 表增加次卡字段
ALTER TABLE "stringing_order" 
ADD COLUMN IF NOT EXISTS "use_punch_card" BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS "punch_card_id" BIGINT;

COMMENT ON COLUMN "stringing_order"."use_punch_card" IS '是否使用次卡';
COMMENT ON COLUMN "stringing_order"."punch_card_id" IS '使用的次卡ID';

