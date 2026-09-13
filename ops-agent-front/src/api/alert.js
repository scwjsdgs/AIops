import request from '@/utils/request'

export const sendAlert = (data) => {
  return request.post('/alerts', data)
}

export const getAlerts = (params) => {
  return request.get('/alerts', { params })
}