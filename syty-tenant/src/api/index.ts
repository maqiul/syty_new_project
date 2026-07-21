/**
 * API 统一入口 — 租户端专属
 */

// --- 公共模块 ---
export * from './common/auth'
export * from './common/base'

// --- 门店管控 (V2.0) ---
export * from './shop'

// --- 租户业务端 ---
export * from './tenant/user'
export * from './tenant/order'
export * from './tenant/tournament'
export * from './tenant/shop'
export * from './tenant/inventory'
export * from './tenant/printer'
export * from './tenant/finance'
export * from './tenant/dashboard'
export * from './tenant/performance'
export * from './tenant/log'
export * from './tenant/supplier'
