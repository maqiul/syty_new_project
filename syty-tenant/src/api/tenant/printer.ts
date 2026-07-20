import request from '../../utils/axios'

export interface PrintTemplate {
  id?: number; name: string; type?: string; content: string; isDefault?: number; status?: number; createdAt?: string
}
export interface PrintPolicy {
  id?: number; name: string; shopId?: number; templateId?: number; triggerType?: string; status?: number; createdAt?: string
}
export interface PrintRule {
  id?: number; policyId: number; condition: string; action: string; sortOrder?: number; createdAt?: string
}
export interface PrinterRegister {
  id?: number; name: string; mqttClientId: string; status?: number; lastHeartbeat?: string; createdAt?: string
}
export interface PrintResource {
  id?: number; name: string; type?: string; url: string; key?: string; createdAt?: string
}

// ============ 打印模板 ============
export function getPrintTemplateList(params?: any) { return request.get('/print-template/list', { params }) }
export function getPrintTemplatePage(params?: any) { return request.get('/print-template/page', { params }) }
export function getPrintTemplate(id: number) { return request.get(`/print-template/${id}`) }
export function addPrintTemplate(data: PrintTemplate) { return request.post('/print-template', data) }
export function updatePrintTemplate(data: PrintTemplate) { return request.put(`/print-template/${data.id}`, data) }
export function deletePrintTemplate(id: number) { return request.delete(`/print-template/${id}`) }

// ============ 打印策略 ============
export function getPrintPolicyList(params?: any) { return request.get('/print-policy/list', { params }) }
export function getPrintPolicyPage(params?: any) { return request.get('/print-policy/page', { params }) }
export function addPrintPolicy(data: PrintPolicy) { return request.post('/print-policy', data) }
export function updatePrintPolicy(data: PrintPolicy) { return request.put(`/print-policy/${data.id}`, data) }
export function deletePrintPolicy(id: number) { return request.delete(`/print-policy/${id}`) }

// ============ 打印规则 ============
export function getPrintRuleList(policyId: number) { return request.get(`/print-policy/${policyId}/rules`) }
export function addPrintRule(policyId: number, data: PrintRule) { return request.post(`/print-policy/${policyId}/rules`, data) }
export function updatePrintRule(data: PrintRule) { return request.put(`/print-rule/${data.id}`, data) }
export function deletePrintRule(id: number) { return request.delete(`/print-rule/${id}`) }

// ============ 打印机注册 ============
export function getPrinterList(params?: any) { return request.get('/printer/list', { params }) }
export function registerPrinter(data: PrinterRegister) { return request.post('/printer/register', data) }
export function updatePrinter(data: PrinterRegister) { return request.put(`/printer/${data.id}`, data) }
export function deletePrinter(id: number) { return request.delete(`/printer/${id}`) }
export function setDefaultPrinter(id: number) { return request.put(`/printer/${id}/default`) }

// ============ 打印规则 ============
export function getPrintRulePage(params?: any) { return request.get('/print-rule/page', { params }) }

// ============ 打印素材 ============
export function getPrintResourceList(params?: any) { return request.get('/print-resource/list', { params }) }
export function uploadPrintResource(data: FormData) { return request.post('/print-resource/upload', data, { headers: { 'Content-Type': 'multipart/form-data' } }) }
export function addPrintResource(data: PrintResource) { return request.post('/print-resource', data) }
export function updatePrintResource(data: PrintResource) { return request.put(`/print-resource/${data.id}`, data) }
export function deletePrintResource(id: number) { return request.delete(`/print-resource/${id}`) }
