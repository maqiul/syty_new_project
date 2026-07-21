import request from '../../utils/axios'

/** 获取公告列表 */
export function getNoticeList(page = 1, size = 10) {
  return request.get('/platform/notices', { params: { page, size } })
}

/** 新增公告 */
export function addNotice(data: { title: string; content: string }) {
  return request.post('/platform/notices', data)
}

/** 发布公告 */
export function publishNotice(id: number) {
  return request.put(`/platform/notices/${id}/publish`)
}
