/**
 * Fluxo de recuperação e redefinição de senha do cliente.
 * Solicita token por e-mail e conclui a troca de senha.
 */
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { authApi } from '../../services/resources'
import { emailRealOk, MSG_EMAIL_INVALIDO } from '../../utils/email'

export default function PortalRecuperarSenhaPage() {
  /** === Estado === */
  const [searchParams] = useSearchParams()
  const [email, setEmail] = useState('')
  const [token, setToken] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [tokenDev, setTokenDev] = useState('')
  const [step, setStep] = useState(1)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  /** === Token na URL === */
  useEffect(() => {
    const t = searchParams.get('token')
    if (t) {
      setToken(t)
      setStep(2)
      setMessage('Informe a nova senha para concluir a redefinição.')
    }
  }, [searchParams])

  /** === Solicitar e redefinir === */
  const solicitar = async (e) => {
    e.preventDefault()
    setError('')
    if (!emailRealOk(email)) {
      setError(MSG_EMAIL_INVALIDO)
      return
    }
    try {
      const { data } = await authApi.recuperarSenha({ email })
      setTokenDev(data.tokenDev || '')
      if (data.tokenDev) setToken(data.tokenDev)
      setMessage(data.mensagem || 'Verifique seu e-mail para o link de redefinição.')
      setStep(2)
      if (data.emailEnviado === false && data.smtpConfigurado === false && !data.tokenDev) {
        setError(
          'O servidor ainda não tem SMTP configurado. Peça ao administrador para definir MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD.',
        )
      }
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Falha ao solicitar recuperação')
    }
  }

  const redefinir = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await authApi.redefinirSenha({ token, novaSenha })
      setMessage('Senha redefinida. Faça login.')
      setStep(3)
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Falha ao redefinir senha')
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
        <h1>Recuperar senha</h1>
        <p className="subtitle">Enviaremos um link para o e-mail cadastrado.</p>

        {/* === Passo 1: solicitar === */}
        {step === 1 && (
          <form onSubmit={solicitar}>
            <label>
              Email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </label>
            {error && <div className="error">{error}</div>}
            <button className="btn" type="submit">Enviar</button>
          </form>
        )}

        {/* === Passo 2: redefinir === */}
        {step === 2 && (
          <form onSubmit={redefinir}>
            {message && <p className="subtitle">{message}</p>}
            {tokenDev && <p className="subtitle">Token (dev): <code>{tokenDev}</code></p>}
            <label>
              Token
              <input value={token} onChange={(e) => setToken(e.target.value)} required />
            </label>
            <label>
              Nova senha (mín. 8, maiúscula, minúscula e número)
              <input
                type="password"
                value={novaSenha}
                onChange={(e) => setNovaSenha(e.target.value)}
                required
                minLength={8}
                pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}"
                title="Mínimo 8 caracteres com maiúscula, minúscula e número"
              />
            </label>
            {error && <div className="error">{error}</div>}
            <button className="btn" type="submit">Redefinir</button>
          </form>
        )}

        {/* === Passo 3: concluído === */}
        {step === 3 && (
          <div>
            <p>{message}</p>
            <Link className="btn" to="/portal/login">Ir para login</Link>
          </div>
        )}

        <div className="auth-toggle">
          <Link to="/portal/login">Voltar</Link>
        </div>
      </div>
    </div>
  )
}
