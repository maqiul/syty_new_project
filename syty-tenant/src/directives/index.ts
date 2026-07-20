import type { App } from 'vue'
import { vHasPermi, vHasRole } from './permission'

/** 注册所有自定义指令 */
export function registerDirectives(app: App): void {
  app.directive('hasPermi', vHasPermi)
  app.directive('hasRole', vHasRole)
}

// 也提供具名导出，方便单独使用
export { vHasPermi, vHasRole }
