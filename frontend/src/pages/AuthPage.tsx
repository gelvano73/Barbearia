/**
 * Página de login e registro do painel administrativo.
 * Login por e-mail/CPF + senha forte, ou código OTP no telefone.
 */
import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import AceitePrivacidade from '../components/AceitePrivacidade'
import { useAuth } from '../context/AuthContext'
import { emailRealOk, MSG_EMAIL_INVALIDO } from '../utils/email'

function senhaForteOk(senha: string) {
  return (
    senha.length >= 8 &&
    /[A-Z]/.test(senha) &&
    /[a-z]/.test(senha) &&
    /\d/.test(senha)
  )
}

export default function AuthPage() {
  const { isAuthenticated, isCliente, isBarbeiro, isAtendente, login, registro, enviarOtp, loginOtp } =
    useAuth()
  const [mode, setMode] = useState('login')
  const [otpMode, setOtpMode] = useState(false)
  const [otpSent, setOtpSent] = useState(false)
  const [otpHint, setOtpHint] = useState('')
  const [codigo, setCodigo] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [aceitePrivacidade, setAceitePrivacidade] = useState(false)
  const [form, setForm] = useState({
    login: '',
    email: '',
    cpf: '',
    senha: '',
    nomeAdmin: '',
    nomeBarbearia: '',
    telefoneBarbearia: '',
    cnpj: '',
  })

  if (isAuthenticated && isCliente) return <Navigate to="/portal" replace />
  if (isAuthenticated && isBarbeiro) return <Navigate to="/barbeiro" replace />
  if (isAuthenticated && isAtendente) return <Navigate to="/recepcao" replace />
  if (isAuthenticated) return <Navigate to="/" replace />

  const onChange = (e) => setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (mode === 'registro' && !aceitePrivacidade) {
      setError('Aceite a Política de Privacidade para continuar.')
      return
    }
    if (mode === 'registro' && !senhaForteOk(form.senha)) {
      setError('Senha: mínimo 8 caracteres, com maiúscula, minúscula e número.')
      return
    }
    if (mode === 'registro' && !emailRealOk(form.email)) {
      setError(MSG_EMAIL_INVALIDO)
      return
    }
    setLoading(true)
    try {
      if (mode === 'login' && otpMode) {
        if (!otpSent) {
          const res = await enviarOtp(form.login.trim())
          setOtpSent(true)
          setOtpHint(res.telefoneMascarado ? `Código enviado para ${res.telefoneMascarado}` : 'Código enviado')
        } else {
          await loginOtp(form.login.trim(), codigo.trim())
        }
      } else if (mode === 'login') {
        await login(form.login.trim(), form.senha)
      } else {
        await registro({
          email: form.email.trim(),
          cpf: form.cpf.replace(/\D/g, ''),
          senha: form.senha,
          nomeAdmin: form.nomeAdmin.trim(),
          nomeBarbearia: form.nomeBarbearia.trim(),
          telefoneBarbearia: form.telefoneBarbearia.trim(),
          cnpj: form.cnpj.replace(/\D/g, '') || undefined,
          aceitePrivacidade: true,
        })
      }
    } catch (err) {
      const detalhes = err.response?.data?.detalhes
      const msg =
        (Array.isArray(detalhes) && detalhes.length ? detalhes.join(' · ') : null) ||
        err.response?.data?.mensagem ||
        err.response?.data?.erro ||
        'Falha na autenticação'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="brand">
          BARBA
          <span>SAAS</span>
        </div>
        <h1>{mode === 'login' ? 'Acesse sua barbearia' : 'Abra sua conta'}</h1>
        <p className="subtitle">Gestão de clientes, equipe e agenda em um só lugar.</p>

        <form onSubmit={onSubmit}>
          {mode === 'registro' && (
            <>
              <label>
                Nome da barbearia
                <input name="nomeBarbearia" value={form.nomeBarbearia} onChange={onChange} required />
              </label>
              <label>
                Seu nome
                <input name="nomeAdmin" value={form.nomeAdmin} onChange={onChange} required />
              </label>
              <label>
                Telefone (WhatsApp)
                <input name="telefoneBarbearia" value={form.telefoneBarbearia} onChange={onChange} required />
              </label>
              <label>
                CPF
                <input name="cpf" value={form.cpf} onChange={onChange} required placeholder="000.000.000-00" />
              </label>
              <label>
                CNPJ
                <input name="cnpj" value={form.cnpj} onChange={onChange} placeholder="00.000.000/0000-00" />
              </label>
              <label>
                Email real (não temporário)
                <input type="email" name="email" value={form.email} onChange={onChange} required placeholder="voce@provedor.com" />
              </label>
            </>
          )}

          {mode === 'login' && (
            <label>
              Email ou CPF
              <input name="login" value={form.login} onChange={onChange} required autoComplete="username" />
            </label>
          )}

          {mode === 'login' && otpMode ? (
            otpSent && (
              <label>
                Código recebido no telefone
                <input value={codigo} onChange={(e) => setCodigo(e.target.value)} required inputMode="numeric" />
              </label>
            )
          ) : (
            <label>
              Senha (mín. 8, maiúscula, minúscula e número)
              <input
                type="password"
                name="senha"
                value={form.senha}
                onChange={onChange}
                required={mode === 'registro' || !otpMode}
                minLength={8}
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              />
            </label>
          )}

          {mode === 'registro' && (
            <AceitePrivacidade checked={aceitePrivacidade} onChange={setAceitePrivacidade} />
          )}
          {otpHint && <p className="subtitle">{otpHint}</p>}
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" disabled={loading}>
            {loading
              ? 'Aguarde...'
              : mode === 'login' && otpMode
                ? otpSent
                  ? 'Validar código'
                  : 'Enviar código'
                : mode === 'login'
                  ? 'Entrar'
                  : 'Criar conta'}
          </button>
        </form>

        {mode === 'login' && (
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
        )}

        <div className="auth-toggle">
          {mode === 'login' ? (
            <button type="button" onClick={() => setMode('registro')}>
              Abrir conta
            </button>
          ) : (
            <button type="button" onClick={() => setMode('login')}>
              Já tenho conta
            </button>
          )}
          <div style={{ marginTop: '0.65rem' }}>
            <a href="/portal/login">Sou cliente</a>
            {' · '}
            <a href="/recepcao/login">Recepção</a>
            {' · '}
            <a href="/privacidade">Privacidade</a>
          </div>
        </div>
      </div>
    </div>
  )
}
