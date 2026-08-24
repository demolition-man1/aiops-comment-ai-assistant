import axios from 'axios'
import { ElMessage } from 'element-plus'

import { i18n } from '@/i18n'

import type { ApiResult } from './types'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

export const http = axios.create({
  baseURL: apiBaseUrl,
  timeout: 300000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('aiops_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>
    if (typeof result?.code === 'number') {
      if (result.code === 200) {
        return result.data as never
      }
      const message = result.msg || i18n.global.t('common.requestFailed')
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return response.data
  },
  (error) => {
    const message = error?.response?.data?.msg || error?.message || i18n.global.t('common.networkFailed')
    if (error?.response?.status === 401) {
      localStorage.removeItem('aiops_token')
      localStorage.removeItem('aiops_user')
      window.location.href = '/login'
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export function uploadFile<T>(url: string, formData: FormData) {
  return http.post<T, T>(url, formData, {
    timeout: 300000
  })
}
