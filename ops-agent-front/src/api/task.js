import request from '@/utils/request'

export const getTask = (id) => {
  return request.get(`/tasks/${id}`)
}