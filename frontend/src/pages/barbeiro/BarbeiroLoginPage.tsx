/**
 * Tela de login do portal do barbeiro.
 */
import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function BarbeiroLoginPage() {
  const { isAuthenticated, isBarbeiro, loginBarbeiro } = useAuth()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (isAuthenticated && isBarbeiro) return <Navigate to="/barbeiro" replace />
  if (isAuthenticated && !isBarbeiro) return <Navigate to="/" replace />

  // Submete o formulário
  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await loginBarbeiro(email, senha)
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
          <span>BARBEIRO</span>
        </div>
        <h1>Portal do barbeiro</h1>
        <p className="subtitle">Agenda, comissões, metas e avaliações.</p>
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
        <div className="auth-toggle">
          <Link to="/auth">Painel da barbearia</Link>
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
