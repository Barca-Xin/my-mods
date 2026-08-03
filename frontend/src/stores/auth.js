import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('mods_token') || ''
  }),
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('mods_token', token)
    },
    logout() {
      this.token = ''
      localStorage.removeItem('mods_token')
    }
  }
})
