import request from '@/utils/request'

export const getTools = () => {
  return request.get('/tools/list')
}