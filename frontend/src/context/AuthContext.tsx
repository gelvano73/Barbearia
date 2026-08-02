/**
 * Contexto de autenticação da aplicação.
 * Gerencia sessão (token/role), login por papel, OAuth, persistência e logout.
 */
import {
  createContext,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { authApi } from '../services/resources'

/** Papéis possíveis na sessão autenticada. */
export type AuthRole = 'ADMIN' | 'ATENDENTE' | 'BARBEIRO' | 'CLIENTE'

/** Dados da sessão guardados em memória e no localStorage. */
export type AuthSession = {
  token: string
  role?: AuthRole
  nome?: string
  email?: string
  nomeBarbearia?: string
  barbeariaId?: number
  clienteId?: number
  barbeiroId?: number
  [key: string]: unknown
}

type AuthContextValue = {
  auth: AuthSession | null
  isAuthenticated: boolean
  isCliente: boolean
  isBarbeiro: boolean
  isAtendente: boolean
  isAdmin: boolean
  isStaff: boolean
  login: (login: string, senha: string) => Promise<AuthSession>
  registro: (payload: Record<string, unknown>) => Promise<AuthSession>
  loginCliente: (login: string, senha: string) => Promise<AuthSession>
  registroCliente: (payload: Record<string, unknown>) => Promise<AuthSession>
  loginBarbeiro: (login: string, senha: string) => Promise<AuthSession>
  loginRecepcao: (login: string, senha: string) => Promise<AuthSession>
  loginOtp: (login: string, codigo: string) => Promise<AuthSession>
  enviarOtp: (login: string) => Promise<{ telefoneMascarado?: string; mensagem?: string }>
  loginOAuth: (provider: string, payload: Record<string, unknown>) => Promise<AuthSession>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)
const STORAGE_KEY = 'barbearia_auth'

function loadStored(): AuthSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as AuthSession) : null
  } catch {
    return null
  }
}

function persist(data: AuthSession) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  return data
}

/** Provedor que expõe estado e ações de autenticação para toda a árvore. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthSession | null>(loadStored)

  const setSession = (data: AuthSession) => {
    setAuth(data)
    persist(data)
    return data
  }

  const login = async (loginId: string, senha: string) => {
    const { data } = await authApi.login({ login: loginId, senha })
    return setSession(data as AuthSession)
  }

  const registro = async (payload: Record<string, unknown>) => {
    const { data } = await authApi.registro(payload)
    return setSession(data as AuthSession)
  }

  const loginCliente = async (loginId: string, senha: string) => {
    const { data } = await authApi.loginCliente({ login: loginId, senha })
    return setSession(data as AuthSession)
  }

  const registroCliente = async (payload: Record<string, unknown>) => {
    const { data } = await authApi.registroCliente(payload)
    return setSession(data as AuthSession)
  }

  const loginBarbeiro = async (loginId: string, senha: string) => {
    const { data } = await authApi.loginBarbeiro({ login: loginId, senha })
    return setSession(data as AuthSession)
  }

  const loginRecepcao = async (loginId: string, senha: string) => {
    const { data } = await authApi.loginRecepcao({ login: loginId, senha })
    return setSession(data as AuthSession)
  }

  const enviarOtp = async (loginId: string) => {
    const { data } = await authApi.enviarOtp({ login: loginId })
    return data as { telefoneMascarado?: string; mensagem?: string }
  }

  const loginOtp = async (loginId: string, codigo: string) => {
    const { data } = await authApi.verificarOtp({ login: loginId, codigo })
    return setSession(data as AuthSession)
  }

  const loginOAuth = async (provider: string, payload: Record<string, unknown>) => {
    const { data } = await authApi.oauth(provider, payload)
    return setSession(data as AuthSession)
  }

  const logout = () => {
    setAuth(null)
    localStorage.removeItem(STORAGE_KEY)
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      auth,
      isAuthenticated: Boolean(auth?.token),
      isCliente: auth?.role === 'CLIENTE',
      isBarbeiro: auth?.role === 'BARBEIRO',
      isAtendente: auth?.role === 'ATENDENTE',
      isAdmin: auth?.role === 'ADMIN',
      isStaff: auth?.role === 'ADMIN',
      login,
      registro,
      loginCliente,
      registroCliente,
      loginBarbeiro,
      loginRecepcao,
      loginOtp,
      enviarOtp,
      loginOAuth,
      logout,
    }),
    [auth],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

/** Hook para consumir o contexto de autenticação. */
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return ctx
}
