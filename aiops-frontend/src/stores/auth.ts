import { defineStore } from 'pinia'

import { authApi } from '@/api/modules'
import type { LoginUser } from '@/api/types'

interface AuthState {
  token: string
  user: LoginUser | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('aiops_token') || '',
    user: readUser()
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(username: string, password: string) {
      const user = await authApi.login({ username, password })
      this.token = user.token
      this.user = user
      localStorage.setItem('aiops_token', user.token)
      localStorage.setItem('aiops_user', JSON.stringify(user))
    },
    async loadProfile() {
      if (!this.token) return
      this.user = await authApi.profile()
      localStorage.setItem('aiops_user', JSON.stringify(this.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('aiops_token')
      localStorage.removeItem('aiops_user')
    }
  }
})

function readUser() {
  const text = localStorage.getItem('aiops_user')
  if (!text) return null
  try {
    return JSON.parse(text) as LoginUser
  } catch {
    return null
  }
}
