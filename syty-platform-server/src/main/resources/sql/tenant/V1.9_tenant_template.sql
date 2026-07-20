-- =============================================================================
-- V1.9 租户 Schema 模板 DDL
-- 数据库: PostgreSQL 12+
-- 执行方式: SET search_path TO tenant_{code}; 然后执行此脚本
-- 维护者: 老K (架构师)
-- 更新日期: 2026-05-12
-- =============================================================================
-- 说明: 此文件仅包含租户私有表 (在 tenant_xxx schema 下创建)。
--       public schema 的全局表 (如 sys_user_auth_index) 由平台端统一管理，
--       不在此文件中定义。
-- =============================================================================

-- =============================================================================
-- 租户用户表 (sys_user)
-- 用途: 存储租户内部员工/穿线师等业务用户
-- =============================================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(64)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    real_name       VARCHAR(50),
    phone           VARCHAR(20),
    role_type       VARCHAR(32)     NOT NULL DEFAULT 'STAFF',
    status          SMALLINT        NOT NULL DEFAULT 1,
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- 约束: role_type 限定为租户内部角色
    CONSTRAINT chk_tenant_user_role CHECK (
        role_type IN ('STAFF', 'THREADER', 'TENANT_ADMIN')
    )
);

-- 索引: 按角色类型查询 (如查所有穿线师)
CREATE INDEX IF NOT EXISTS idx_tenant_user_role_type
    ON sys_user (role_type);

-- 索引: 状态过滤
CREATE INDEX IF NOT EXISTS idx_tenant_user_status
    ON sys_user (status);

-- 注释
COMMENT ON TABLE sys_user IS
    '租户用户表 — 存储租户内部员工/穿线师等业务用户, 属于租户独立 schema (tenant_xxx)';
COMMENT ON COLUMN sys_user.id IS '主键 ID (自增)';
COMMENT ON COLUMN sys_user.username IS '登录用户名 (租户内唯一)';
COMMENT ON COLUMN sys_user.password_hash IS '密码哈希 (BCrypt)';
COMMENT ON COLUMN sys_user.real_name IS '真实姓名';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.role_type IS '角色类型: STAFF=普通员工, THREADER=穿线师, TENANT_ADMIN=租户管理员';
COMMENT ON COLUMN sys_user.status IS '状态: 1=启用, 0=禁用';
COMMENT ON COLUMN sys_user.deleted IS '逻辑删除: 0=未删除, 1=已删除';
COMMENT ON COLUMN sys_user.created_at IS '创建时间';
COMMENT ON COLUMN sys_user.updated_at IS '更新时间';
