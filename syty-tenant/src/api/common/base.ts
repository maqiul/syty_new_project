import request from '../../utils/axios'

/** 文件上传 */
export function uploadFile(data: FormData) {
  return request.post('/file/upload', data, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export interface Player {
  id?: number
  name: string
  phone?: string
  gender?: string
  level?: string
  sportType?: string  // BADMINTON | TENNIS | DUAL
  ranking?: string    // 网球排名
  remark?: string
  createdAt?: string
}

export interface Racket {
  id?: number
  brand: string
  model: string
  sportType?: string  // BADMINTON | TENNIS
  gripSize?: string
  weight?: string
  balance?: string
  headSize?: string      // 网球拍面
  stringPattern?: string // 网球线床
  createdAt?: string
}

export interface StringInfo {
  id?: number
  brand: string
  model: string
  sportType?: string  // BADMINTON | TENNIS
  type?: string
  gauge?: string
  color?: string
  createdAt?: string
}

// 网球相关（兼容类型，后续删除）
export type TennisPlayer = Player
export type TennisRacket = Racket
export type TennisString = StringInfo

// ============ 球员（统一接口，支持 sportType 过滤） ============
export function getPlayerPage(params?: any) { return request.get('/player/page', { params }) }
export function getPlayerList(params?: any) { return request.get('/player/list', { params }) }
export function getPlayer(id: number) { return request.get(`/player/${id}`) }
export function addPlayer(data: Player) { return request.post('/player', data) }
export function updatePlayer(data: Player) { return request.put(`/player/${data.id}`, data) }
export function deletePlayer(id: number) { return request.delete(`/player/${id}`) }

// ============ 球拍（统一接口，支持 sportType 过滤） ============
export function getRacketPage(params?: any) { return request.get('/racket/page', { params }) }
export function getRacketList(params?: any) { return request.get('/racket/list', { params }) }
export function getRacket(id: number) { return request.get(`/racket/${id}`) }
export function addRacket(data: Racket) { return request.post('/racket', data) }
export function updateRacket(data: Racket) { return request.put(`/racket/${data.id}`, data) }
export function deleteRacket(id: number) { return request.delete(`/racket/${id}`) }

// ============ 线材（统一接口，支持 sportType 过滤） ============
export function getStringInfoPage(params?: any) { return request.get('/string-info/page', { params }) }
export function getStringInfoList(params?: any) { return request.get('/string-info/list', { params }) }
export function getStringInfo(id: number) { return request.get(`/string-info/${id}`) }
export function addStringInfo(data: StringInfo) { return request.post('/string-info', data) }
export function updateStringInfo(data: StringInfo) { return request.put(`/string-info/${data.id}`, data) }
export function deleteStringInfo(id: number) { return request.delete(`/string-info/${id}`) }

// ============ 网球基础数据（兼容层，代理到统一接口） ============
// @deprecated 后续版本删除，请使用统一接口 + sportType='TENNIS'
export function getTennisPlayerPage(params?: any) { return getPlayerPage({ ...params, sportType: 'TENNIS' }) }
export function getTennisPlayerList(params?: any) { return getPlayerList({ ...params, sportType: 'TENNIS' }) }
export function addTennisPlayer(data: TennisPlayer) { return addPlayer({ ...data, sportType: 'TENNIS' }) }
export function updateTennisPlayer(data: TennisPlayer) { return updatePlayer({ ...data, sportType: 'TENNIS' }) }
export function deleteTennisPlayer(id: number) { return deletePlayer(id) }

export function getTennisRacketPage(params?: any) { return getRacketPage({ ...params, sportType: 'TENNIS' }) }
export function getTennisRacketList(params?: any) { return getRacketList({ ...params, sportType: 'TENNIS' }) }
export function addTennisRacket(data: TennisRacket) { return addRacket({ ...data, sportType: 'TENNIS' }) }
export function updateTennisRacket(data: TennisRacket) { return updateRacket({ ...data, sportType: 'TENNIS' }) }
export function deleteTennisRacket(id: number) { return deleteRacket(id) }

export function getTennisStringPage(params?: any) { return getStringInfoPage({ ...params, sportType: 'TENNIS' }) }
export function getTennisStringList(params?: any) { return getStringInfoList({ ...params, sportType: 'TENNIS' }) }
export function addTennisString(data: TennisString) { return addStringInfo({ ...data, sportType: 'TENNIS' }) }
export function updateTennisString(data: TennisString) { return updateStringInfo({ ...data, sportType: 'TENNIS' }) }
export function deleteTennisString(id: number) { return deleteStringInfo(id) }
