/**
 * Login do portal do cliente: e-mail/CPF + senha, OTP no telefone, ou OAuth.
 */
import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function PortalLoginPage() {
  /** === Estado === */
  const { isAuthenticated, isCliente, loginCliente, loginOAuth, enviarOtp, loginOtp } = useAuth()
  const [loginId, setLoginId] = useState('')
  const [senha, setSenha] = useState('')
  const [otpMode, setOtpMode] = useState(false)
  const [otpSent, setOtpSent] = useState(false)
  const [otpHint, setOtpHint] = useState('')
  const [codigo, setCodigo] = useState('')
  const [barbeariaId, setBarbeariaId] = useState('1')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  /** === Redirecionamentos === */
  if (isAuthenticated && isCliente) return <Navigate to="/portal" replace />
  if (isAuthenticated && !isCliente) return <Navigate to="/" replace />

  /** === Login e OTP === */
  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      if (otpMode) {
        if (!otpSent) {
          const res = await enviarOtp(loginId.trim())
          setOtpSent(true)
          setOtpHint(res.telefoneMascarado ? `Código enviado para ${res.telefoneMascarado}` : 'Código enviado')
        } else {
          await loginOtp(loginId.trim(), codigo.trim())
        }
      } else {
        await loginCliente(loginId.trim(), senha)
      }
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Falha no login')
    } finally {
      setLoading(false)
    }
  }

  /** === OAuth === */
  const social = async (provider) => {
    setError('')
    setLoading(true)
    try {
      await loginOAuth(provider, {
        providerUserId: `${provider}-dev-${loginId || 'demo'}`,
        email: loginId.includes('@') ? loginId : `demo.${provider}@cliente.com`,
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
        {/* === Cabeçalho === */}
        <div className="brand">
          BARBA
          <span>PORTAL</span>
        </div>
        <h1>Área do cliente</h1>
        <p className="subtitle">Agende, acompanhe e avalie seus atendimentos.</p>

        {/* === Formulário === */}
        <form onSubmit={onSubmit}>
          <label>
            Email ou CPF
            <input value={loginId} onChange={(e) => setLoginId(e.target.value)} required autoComplete="username" />
          </label>
          {otpMode ? (
            otpSent && (
              <label>
                Código recebido no telefone
                <input value={codigo} onChange={(e) => setCodigo(e.target.value)} required inputMode="numeric" />
              </label>
            )
          ) : (
            <label>
              Senha (mín. 8, maiúscula, minúscula e número)
              <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required minLength={8} />
            </label>
          )}
          {otpHint && <p className="subtitle">{otpHint}</p>}
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" disabled={loading}>
            {loading
              ? 'Aguarde...'
              : otpMode
                ? otpSent
                  ? 'Validar código'
                  : 'Enviar código'
                : 'Entrar'}
          </button>
        </form>

        <button
          type="button"
          className="btn secondary"
          style={{ marginTop: '0.75rem', width: '100%' }}
          onClick={() => {
            setOtpMode((v) => !v)
            setOtpSent(false)
            setCodigo('')
            setOtpHint('')
            setError('')
          }}
        >
          {otpMode ? 'Entrar com senha' : 'Entrar com código no telefone'}
        </button>

        {/* === Login social === */}
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

        {/* === Links === */}
        <div className="auth-toggle">
          <Link to="/portal/registro">Criar conta</Link>
          {' · '}
          <Link to="/recuperar-senha?voltar=/portal/login">Esqueci minha senha</Link>
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
