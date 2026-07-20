import request from '../../utils/axios'

export interface Tournament {
  id?: number
  name: string
  startDate?: string
  endDate?: string
  location?: string
  status?: number
  remark?: string
  createdAt?: string
}

// ============ 赛事管理 ============
export function getTournamentPage(params?: any) { return request.get('/tournament/page', { params }) }
export function getTournament(id: number) { return request.get(`/tournament/${id}`) }
export function addTournament(data: Tournament) { return request.post('/tournament', data) }
export function updateTournament(data: Tournament) { return request.put(`/tournament/${data.id}`, data) }
export function deleteTournament(id: number) { return request.delete(`/tournament/${id}`) }

// ============ 羽毛球赛事订单 ============
export function getBadmintonTournamentPage(params?: any) { return request.get('/badminton-tournament/page', { params }) }
export function addBadmintonTournamentOrder(data: any) { return request.post('/badminton-tournament', data) }
export function updateBadmintonTournamentOrder(data: any) { return request.put('/badminton-tournament', data) }
export function deleteBadmintonTournamentOrder(id: number) { return request.delete(`/badminton-tournament/${id}`) }

export const createBadmintonTournamentOrder = addBadmintonTournamentOrder
// 羽毛球赛事暂无 complete 接口，使用 update 替代
export function completeBadmintonTournamentOrder(id: number) {
  return request.put('/badminton-tournament', { id, status: 2 })
}

// ============ 网球赛事订单 ============
export function getTennisTournamentPage(params?: any) { return request.get('/tennis/tournament/page', { params }) }
export function addTennisTournamentOrder(data: any) { return request.post('/tennis/tournament', data) }
export function updateTennisTournamentOrder(data: any) { return request.put('/tennis/tournament', data) }
export function deleteTennisTournamentOrder(id: number) { return request.delete(`/tennis/tournament/${id}`) }

// 网球赛事暂无 complete 接口，使用 update 替代
export function completeTennisTournamentOrder(id: number) {
  return request.put('/tennis/tournament', { id, status: 2 })
}

export const createTennisTournamentOrder = addTennisTournamentOrder
