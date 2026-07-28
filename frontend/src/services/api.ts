/**
 * Instância Axios compartilhada para chamadas à API do backend.
 * Define a baseURL e anexa automaticamente o token JWT do localStorage.
 */
import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
})

const STORAGE_KEY = 'barbearia_auth'

// Interceptor: injeta Bearer token da sessão salva em localStorage
api.interceptors.request.use((config) => {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (raw) {
    try {
      const { token } = JSON.parse(raw) as { token?: string }
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    } catch {
      /* sessão inválida no storage */
    }
  }
  return config
})

// Interceptor: sessão expirada / sem permissão
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    if (status === 401 || status === 403) {
      const data = error.response?.data
      const mensagem =
        (typeof data === 'object' && data?.mensagem) ||
        'Sessão expirada ou sem permissão. Faça login novamente.'

      if (typeof data !== 'object' || !data?.mensagem) {
        error.response = error.response || {}
        error.response.data = {
          ...(typeof data === 'object' && data ? data : {}),
          mensagem,
        }
      }

      // Token inválido/expirado: limpa sessão e volta ao login (exceto na própria tela de auth)
      if (status === 401 && localStorage.getItem(STORAGE_KEY)) {
        localStorage.removeItem(STORAGE_KEY)
        const path = window.location.pathname
        if (!path.startsWith('/login') && !path.startsWith('/registro')) {
          window.location.assign('/login')
        }
      }
    }
    return Promise.reject(error)
  },
)

export default api
