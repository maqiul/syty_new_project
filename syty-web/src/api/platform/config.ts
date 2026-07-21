import request from '../../utils/axios'

/** 获取平台配置 */
export function getPlatformConfig() {
  return request.get('/platform/config')
}

/** 更新平台配置 */
export function updatePlatformConfig(data: any) {
  return request.put('/platform/config', data)
}
