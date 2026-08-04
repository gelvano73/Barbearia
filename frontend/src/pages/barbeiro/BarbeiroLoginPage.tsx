/**
 * Tela de login do portal do barbeiro.
 * Autentica a conta da equipe e redireciona à área do barbeiro.
 */
import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function BarbeiroLoginPage() {
  /** === Estado === */
  const { isAuthenticated, isBarbeiro, loginBarbeiro } = useAuth()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  /** === Redirecionamentos === */
  if (isAuthenticated && isBarbeiro) return <Navigate to="/barbeiro" replace />
  if (isAuthenticated && !isBarbeiro) return <Navigate to="/" replace />

  /** === Submit === */
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
        {/* === Cabeçalho === */}
        <div className="brand">
          BARBA
          <span>BARBEIRO</span>
        </div>
        <h1>Portal do barbeiro</h1>
        <p className="subtitle">Agenda, comissões, metas e avaliações.</p>
        {/* === Formulário === */}
        <form onSubmit={onSubmit}>
          <label>
            Email ou CPF
            <input value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" />
          </label>
          <label>
            Senha (mín. 8, maiúscula, minúscula e número)
            <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required minLength={8} />
          </label>
          <Link className="auth-forgot" to="/recuperar-senha?voltar=/barbeiro/login">
            Esqueci minha senha
          </Link>
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" disabled={loading}>
            {loading ? 'Aguarde...' : 'Entrar'}
          </button>
        </form>
        {/* === Links === */}
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
