/**
 * Login da área de recepção/atendimento.
 * Autentica atendente ou admin e redireciona ao portal da recepção.
 */
import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import PasswordInput from '../../components/PasswordInput'
import { useAuth } from '../../context/AuthContext'

export default function RecepcaoLoginPage() {
  /** === Estado === */
  const { isAuthenticated, isAtendente, isAdmin, loginRecepcao } = useAuth()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  /** === Redirecionamentos === */
  if (isAuthenticated && (isAtendente || isAdmin)) return <Navigate to="/recepcao" replace />
  if (isAuthenticated) return <Navigate to="/" replace />

  /** === Submit === */
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
        {/* === Cabeçalho === */}
        <div className="brand">
          BARBA
          <span>RECEPÇÃO</span>
        </div>
        <h1>Portal da recepção</h1>
        <p className="subtitle">Agenda, fila, clientes, pagamentos e caixa.</p>
        {/* === Formulário === */}
        <form onSubmit={onSubmit}>
          <label>
            Email ou CPF
            <input value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" />
          </label>
          <label>
            Senha (mín. 8, maiúscula, minúscula e número)
            <PasswordInput
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              required
              minLength={8}
              autoComplete="current-password"
            />
          </label>
          <Link
            className="auth-forgot"
            to={`/recuperar-senha?voltar=/recepcao/login${email.trim() ? `&login=${encodeURIComponent(email.trim())}` : ''}`}
          >
            Esqueci minha senha
          </Link>
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" disabled={loading}>
            {loading ? 'Aguarde...' : 'Entrar'}
          </button>
        </form>
        {/* === Links === */}
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
