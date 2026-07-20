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
  remark?: string
  createdAt?: string
}

export interface Racket {
  id?: number
  brand: string
  model: string
  gripSize?: string
  weight?: string
  balance?: string
  createdAt?: string
}

export interface StringInfo {
  id?: number
  brand: string
  model: string
  type?: string
  gauge?: string
  color?: string
  createdAt?: string
}

// 网球相关
export interface TennisPlayer {
  id?: number; name: string; phone?: string; gender?: string; level?: string; remark?: string; createdAt?: string
}
export interface TennisRacket {
  id?: number; brand: string; model: string; gripSize?: string; weight?: string; balance?: string; createdAt?: string
}
export interface TennisString {
  id?: number; brand: string; model: string; type?: string; gauge?: string; color?: string; createdAt?: string
}

// ============ 球员 ============
export function getPlayerPage(params?: any) { return request.get('/player/page', { params }) }
export function getPlayerList(params?: any) { return request.get('/player/list', { params }) }
export function getPlayer(id: number) { return request.get(`/player/${id}`) }
export function addPlayer(data: Player) { return request.post('/player', data) }
export function updatePlayer(data: Player) { return request.put(`/player/${data.id}`, data) }
export function deletePlayer(id: number) { return request.delete(`/player/${id}`) }

// ============ 球拍 ============
export function getRacketPage(params?: any) { return request.get('/racket/page', { params }) }
export function getRacketList(params?: any) { return request.get('/racket/list', { params }) }
export function getRacket(id: number) { return request.get(`/racket/${id}`) }
export function addRacket(data: Racket) { return request.post('/racket', data) }
export function updateRacket(data: Racket) { return request.put(`/racket/${data.id}`, data) }
export function deleteRacket(id: number) { return request.delete(`/racket/${id}`) }

// ============ 线材 ============
export function getStringInfoPage(params?: any) { return request.get('/string-info/page', { params }) }
export function getStringInfoList(params?: any) { return request.get('/string-info/list', { params }) }
export function getStringInfo(id: number) { return request.get(`/string-info/${id}`) }
export function addStringInfo(data: StringInfo) { return request.post('/string-info', data) }
export function updateStringInfo(data: StringInfo) { return request.put(`/string-info/${data.id}`, data) }
export function deleteStringInfo(id: number) { return request.delete(`/string-info/${id}`) }

// ============ 网球基础数据 ============
export function getTennisPlayerPage(params?: any) { return request.get('/tennis/player/page', { params }) }
export function getTennisPlayerList(params?: any) { return request.get('/tennis/player/list', { params }) }
export function addTennisPlayer(data: TennisPlayer) { return request.post('/tennis/player', data) }
export function updateTennisPlayer(data: TennisPlayer) { return request.put(`/tennis/player/${data.id}`, data) }
export function deleteTennisPlayer(id: number) { return request.delete(`/tennis/player/${id}`) }

export function getTennisRacketPage(params?: any) { return request.get('/tennis/racket/page', { params }) }
export function getTennisRacketList(params?: any) { return request.get('/tennis/racket/list', { params }) }
export function addTennisRacket(data: TennisRacket) { return request.post('/tennis/racket', data) }
export function updateTennisRacket(data: TennisRacket) { return request.put(`/tennis/racket/${data.id}`, data) }
export function deleteTennisRacket(id: number) { return request.delete(`/tennis/racket/${id}`) }

export function getTennisStringPage(params?: any) { return request.get('/tennis/string/page', { params }) }
export function getTennisStringList(params?: any) { return request.get('/tennis/string/list', { params }) }
export function addTennisString(data: TennisString) { return request.post('/tennis/string', data) }
export function updateTennisString(data: TennisString) { return request.put(`/tennis/string/${data.id}`, data) }
export function deleteTennisString(id: number) { return request.delete(`/tennis/string/${id}`) }
