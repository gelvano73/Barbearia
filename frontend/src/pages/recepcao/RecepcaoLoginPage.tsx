/**
 * Login da área de recepção/atendimento.
 */
import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function RecepcaoLoginPage() {
  const { isAuthenticated, isAtendente, isAdmin, loginRecepcao } = useAuth()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (isAuthenticated && (isAtendente || isAdmin)) return <Navigate to="/recepcao" replace />
  if (isAuthenticated) return <Navigate to="/" replace />

  // Submete o formulário
  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await loginRecepcao(email.trim(), senha)
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Falha no login')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="brand">
          BARBA
          <span>RECEPÇÃO</span>
        </div>
        <h1>Portal da recepção</h1>
        <p className="subtitle">Agenda, fila, clientes, pagamentos e caixa.</p>
        <form onSubmit={onSubmit}>
          <label>
            Email ou CPF
            <input value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" />
          </label>
          <label>
            Senha (mín. 8, maiúscula, minúscula e número)
            <input
              type="password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              required
              minLength={8}
            />
          </label>
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" disabled={loading}>
            {loading ? 'Aguarde...' : 'Entrar'}
          </button>
        </form>
        <div className="auth-toggle">
          <Link to="/auth">Painel admin</Link>
          {' · '}
          <Link to="/barbeiro/login">Sou barbeiro</Link>
          {' · '}
          <Link to="/portal/login">Sou cliente</Link>
          <div style={{ marginTop: '0.65rem' }}>
            <Link to="/privacidade">Política de Privacidade</Link>
          </div>
        </div>
      </div>
    </div>
  )
}
