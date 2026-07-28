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
  login: (email: string, senha: string) => Promise<AuthSession>
  registro: (payload: Record<string, unknown>) => Promise<AuthSession>
  loginCliente: (email: string, senha: string) => Promise<AuthSession>
  registroCliente: (payload: Record<string, unknown>) => Promise<AuthSession>
  loginBarbeiro: (email: string, senha: string) => Promise<AuthSession>
  loginRecepcao: (email: string, senha: string) => Promise<AuthSession>
  loginOAuth: (provider: string, payload: Record<string, unknown>) => Promise<AuthSession>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)
const STORAGE_KEY = 'barbearia_auth'

// Lê a sessão previamente salva no navegador
function loadStored(): AuthSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as AuthSession) : null
  } catch {
    return null
  }
}

// Persiste a sessão no localStorage
function persist(data: AuthSession) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  return data
}

/** Provedor que expõe estado e ações de autenticação para toda a árvore. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthSession | null>(loadStored)

  // Atualiza estado e storage com a nova sessão
  const setSession = (data: AuthSession) => {
    setAuth(data)
    persist(data)
    return data
  }

  // Login do admin/staff
  const login = async (email: string, senha: string) => {
    const { data } = await authApi.login({ email, senha })
    return setSession(data as AuthSession)
  }

  // Cadastro de nova barbearia/admin
  const registro = async (payload: Record<string, unknown>) => {
    const { data } = await authApi.registro(payload)
    return setSession(data as AuthSession)
  }

  // Login do cliente no portal
  const loginCliente = async (email: string, senha: string) => {
    const { data } = await authApi.loginCliente({ email, senha })
    return setSession(data as AuthSession)
  }

  // Registro de cliente no portal
  const registroCliente = async (payload: Record<string, unknown>) => {
    const { data } = await authApi.registroCliente(payload)
    return setSession(data as AuthSession)
  }

  // Login do barbeiro
  const loginBarbeiro = async (email: string, senha: string) => {
    const { data } = await authApi.loginBarbeiro({ email, senha })
    return setSession(data as AuthSession)
  }

  // Login da recepção/atendente
  const loginRecepcao = async (email: string, senha: string) => {
    const { data } = await authApi.loginRecepcao({ email, senha })
    return setSession(data as AuthSession)
  }

  // Login via provedor OAuth (Google etc.)
  const loginOAuth = async (provider: string, payload: Record<string, unknown>) => {
    const { data } = await authApi.oauth(provider, payload)
    return setSession(data as AuthSession)
  }

  // Encerra a sessão e limpa o storage
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
      loginOAuth,
      logout,
    }),
    [auth]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

/** Hook para consumir o contexto de autenticação. */
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return ctx
}
