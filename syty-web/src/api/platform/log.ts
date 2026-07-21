import request from '../../utils/axios'

/** 分页查询操作日志 */
export function getOperateLogPage(page = 1, size = 20, module = '', action = '') {
  return request.get('/operate-log/page', { params: { page, size, module, action } })
}
