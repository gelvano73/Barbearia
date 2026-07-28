/**
 * Login do portal do cliente, com opção OAuth quando disponível.
 */
import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function PortalLoginPage() {
  const { isAuthenticated, isCliente, loginCliente, loginOAuth } = useAuth()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [barbeariaId, setBarbeariaId] = useState('1')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (isAuthenticated && isCliente) return <Navigate to="/portal" replace />
  if (isAuthenticated && !isCliente) return <Navigate to="/" replace />

  // Submete o formulário
  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await loginCliente(email, senha)
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Falha no login')
    } finally {
      setLoading(false)
    }
  }

  // Login via provedor social (OAuth)
  const social = async (provider) => {
    setError('')
    setLoading(true)
    try {
      await loginOAuth(provider, {
        providerUserId: `${provider}-dev-${email || 'demo'}`,
        email: email || `demo.${provider}@cliente.com`,
        nome: 'Cliente Social',
        barbeariaId: Number(barbeariaId),
        telefone: '11999999999',
      })
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Falha no login social')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="brand">
          BARBA
          <span>PORTAL</span>
        </div>
        <h1>Área do cliente</h1>
        <p className="subtitle">Agende, acompanhe e avalie seus atendimentos.</p>

        <form onSubmit={onSubmit}>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Senha
            <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required minLength={6} />
          </label>
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" disabled={loading}>
            {loading ? 'Aguarde...' : 'Entrar'}
          </button>
        </form>

        <div style={{ marginTop: '1rem', display: 'grid', gap: '0.5rem' }}>
          <label>
            Barbearia (OAuth dev)
            <input value={barbeariaId} onChange={(e) => setBarbeariaId(e.target.value)} />
          </label>
          <button className="btn secondary" type="button" disabled={loading} onClick={() => social('google')}>
            Entrar com Google
          </button>
          <button className="btn secondary" type="button" disabled={loading} onClick={() => social('facebook')}>
            Entrar com Facebook
          </button>
        </div>

        <div className="auth-toggle">
          <Link to="/portal/registro">Criar conta</Link>
          {' · '}
          <Link to="/portal/recuperar-senha">Esqueci a senha</Link>
          {' · '}
          <Link to="/auth">Sou da barbearia</Link>
          <div style={{ marginTop: '0.65rem' }}>
            <Link to="/privacidade">Política de Privacidade</Link>
          </div>
        </div>
      </div>
    </div>
  )
}
