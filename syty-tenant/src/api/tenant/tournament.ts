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
export function getBadmintonTournamentPage(params?: any) { return request.get('/tournament/badminton/page', { params }) }
export function addBadmintonTournamentOrder(data: any) { return request.post('/tournament/badminton/order', data) }
export function updateBadmintonTournamentOrder(data: any) { return request.put(`/tournament/badminton/order/${data.id}`, data) }
export function completeBadmintonTournamentOrder(id: number) { return request.put(`/tournament/badminton/order/${id}/complete`) }
export function deleteBadmintonTournamentOrder(id: number) { return request.delete(`/tournament/badminton/order/${id}`) }

export const createBadmintonTournamentOrder = addBadmintonTournamentOrder

// ============ 网球赛事订单 ============
export function getTennisTournamentPage(params?: any) { return request.get('/tennis/tournament/page', { params }) }
export function addTennisTournamentOrder(data: any) { return request.post('/tennis/tournament/order', data) }
export function updateTennisTournamentOrder(data: any) { return request.put(`/tennis/tournament/order/${data.id}`, data) }
export function completeTennisTournamentOrder(id: number) { return request.put(`/tennis/tournament/order/${id}/complete`) }
export function deleteTennisTournamentOrder(id: number) { return request.delete(`/tennis/tournament/order/${id}`) }

export const createTennisTournamentOrder = addTennisTournamentOrder
