import axios from 'axios'
import { modApi as staticModApi } from './static'

/**
 * 两种模式：
 * - 默认（VITE_API_MODE 非 static）：走后端 Spring Boot API（本地开发 / 服务器部署）
 * - static：读 data.json 纯静态（GitHub Pages 部署）
 */
export const IS_STATIC = import.meta.env.VITE_API_MODE === 'static'

const http = axios.create({ baseURL: '/api' })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('mods_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401 && !err.config?.url?.includes('/login')) {
      localStorage.removeItem('mods_token')
      if (!window.location.pathname.startsWith('/admin/login')) {
        window.location.href = '/admin/login'
      }
    }
    return Promise.reject(err)
  }
)

const apiModApi = {
  list: (params) => http.get('/mods', { params }),
  detail: (slug) => http.get(`/mods/${slug}`),
  versions: (slug, gameVersion) => http.get(`/mods/${slug}/versions`, { params: { gameVersion } }),
  dependencies: (slug) => http.get(`/mods/${slug}/dependencies`),
  download: (versionId) => http.post(`/downloads/${versionId}`)
}

const apiAdminApi = {
  login: (data) => http.post('/admin/login', data),
  parseJar: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/admin/parse-jar', fd)
  },
  listMods: () => http.get('/admin/mods'),
  createMod: (data) => http.post('/admin/mods', data),
  updateMod: (id, data) => http.put(`/admin/mods/${id}`, data),
  deleteMod: (id) => http.delete(`/admin/mods/${id}`),
  uploadVersion: (modId, data) => {
    const fd = new FormData()
    fd.append('file', data.file)
    if (data.modVersion) fd.append('modVersion', data.modVersion)
    if (data.gameVersion) fd.append('gameVersion', data.gameVersion)
    if (data.modLoader) fd.append('modLoader', data.modLoader)
    if (data.changelog) fd.append('changelog', data.changelog)
    if (data.releaseDate) fd.append('releaseDate', data.releaseDate)
    if (data.recommended !== undefined) fd.append('recommended', String(data.recommended))
    ;(data.dependencies || []).forEach((d) => fd.append('dependencies', d))
    return http.post(`/admin/mods/${modId}/versions`, fd)
  },
  deleteVersion: (modId, versionId) => http.delete(`/admin/mods/${modId}/versions/${versionId}`),
  setRecommended: (modId, versionId, recommended) =>
    http.put(`/admin/mods/${modId}/versions/${versionId}/recommended`, { recommended })
}

export const modApi = IS_STATIC ? staticModApi : apiModApi

/** 静态模式下没有后台，adminApi 为 null（后台路由已被拦截） */
export const adminApi = IS_STATIC ? null : apiAdminApi

export default http
