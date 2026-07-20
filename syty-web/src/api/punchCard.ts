import { defHttp } from '/@/utils/http/axios'

enum Api {
  Issue = '/api/v1/admin/punch-card/issue',
  List = '/api/v1/admin/punch-card/list',
}

export const issueCard = (params: any) => defHttp.post({ url: Api.Issue, params })
export const getCardList = () => defHttp.get({ url: Api.List })
