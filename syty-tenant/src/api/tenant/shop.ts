import request from '../../utils/axios'

export interface Shop {
  id?: number
  name: string
  address?: string
  phone?: string
  contactPerson?: string
  remark?: string
  tenantId?: number
  tenantName?: string
  createdAt?: string
}

export function getShopPage(params?: any) { return request.get('/shop/page', { params }) }
export function getShopList(params?: any) { return request.get('/shop/list', { params }) }
export function getShop(id: number) { return request.get(`/shop/${id}`) }
export function addShop(data: Shop) { return request.post('/shop', data) }
export function updateShop(data: Shop) { return request.put(`/shop/${data.id}`, data) }
export function deleteShop(id: number) { return request.delete(`/shop/${id}`) }

/** 获取当前用户绑定的门店 */
export function getMyShops() { return request.get('/shop/my') }

/** 绑定用户到门店 */
export function bindUserToShop(userId: number, shopId: number) {
  return request.post('/shop/bind-user', { userId, shopId })
}

/** 获取门店绑定的用户 */
export function getShopUsers(shopId: number) {
  return request.get(`/shop/${shopId}/users`)
}

/** 解绑用户 */
export function unbindUserFromShop(userId: number, shopId: number) {
  return request.delete(`/shop/${shopId}/users/${userId}`)
}

/** 穿线师 */
export interface Stringer {
  id?: number
  name: string
  phone?: string
  remark?: string
}

/** 店铺穿线信息 */
export interface ShopString {
  id?: number
  stringInfoId: number
  stringName: string
  price: number
  shopId?: number
}

/** 获取店铺穿线信息列表（待后端实现） */
export function getShopStrings(shopId?: number) { return Promise.resolve({ data: [] as ShopString[] }) }
export function saveShopString(data: ShopString) { return Promise.resolve({ data }) }
export function updateShopString(data: ShopString) { return Promise.resolve({ data }) }
export function deleteShopString(id: number) { return Promise.resolve({ data: null }) }

/** 租户端不需要租户列表，返回空 */
export function getTenantList() { return Promise.resolve({ data: [] }) }

// --- 网球穿线相关（待后端实现） ---
export interface TennisShopString {
  id?: number
  shopId: number
  stringId: number
  price: number
  stock: number
}

export function getTennisShopStrings(shopId?: number) { return Promise.resolve({ data: [] as TennisShopString[] }) }
export function addTennisShopString(data: TennisShopString) { return Promise.resolve({ data }) }
export function updateTennisShopString(data: TennisShopString) { return Promise.resolve({ data }) }
export function deleteTennisShopString(id: number) { return Promise.resolve({ data: null }) }
