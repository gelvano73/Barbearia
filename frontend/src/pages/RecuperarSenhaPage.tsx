/**
 * Recuperação de senha (admin, cliente e demais papéis).
 * Solicita com e-mail ou CPF e conclui a troca com o token.
 */
import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import PasswordInput from '../components/PasswordInput'
import { authApi } from '../services/resources'

type Props = {
  brand?: string
  loginPath?: string
}

const LOGIN_POR_VOLTAR: Record<string, { path: string; brand: string }> = {
  '/auth': { path: '/auth', brand: 'SAAS' },
  '/portal/login': { path: '/portal/login', brand: 'PORTAL' },
  '/barbeiro/login': { path: '/barbeiro/login', brand: 'BARBEIRO' },
  '/recepcao/login': { path: '/recepcao/login', brand: 'RECEPÇÃO' },
}

export default function RecuperarSenhaPage({ brand = 'SAAS', loginPath = '/auth' }: Props) {
  /** === Estado === */
  const [searchParams] = useSearchParams()
  const origem = useMemo(() => {
    const voltar = searchParams.get('voltar') || ''
    return LOGIN_POR_VOLTAR[voltar] || { path: loginPath, brand }
  }, [searchParams, loginPath, brand])

  const [login, setLogin] = useState(() => searchParams.get('login') || '')
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
    const valor = login.trim()
    if (!valor) {
      setError('Informe o e-mail ou CPF cadastrado')
      return
    }
    try {
      const { data } = await authApi.recuperarSenha({ login: valor })
      setTokenDev(data.tokenDev || '')
      if (data.tokenDev) setToken(data.tokenDev)
      setMessage(data.mensagem || 'Se a conta existir, enviaremos o link no e-mail cadastrado.')
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
          <span>{origem.brand}</span>
        </div>
        <h1>Recuperar senha</h1>
        <p className="subtitle">
          Informe o e-mail ou CPF da conta. O link de redefinição chega no e-mail cadastrado.
        </p>

        {/* === Passo 1: solicitar === */}
        {step === 1 && (
          <form onSubmit={solicitar}>
            <label>
              E-mail ou CPF
              <input
                type="text"
                value={login}
                onChange={(e) => setLogin(e.target.value)}
                required
                autoComplete="username"
                placeholder="voce@provedor.com ou 000.000.000-00"
              />
            </label>
            {error && <div className="error">{error}</div>}
            <button className="btn" type="submit">Enviar link</button>
          </form>
        )}

        {/* === Passo 2: redefinir === */}
        {step === 2 && (
          <form onSubmit={redefinir}>
            {message && <p className="subtitle">{message}</p>}
            {tokenDev && (
              <p className="subtitle">
                Token (dev): <code>{tokenDev}</code>
              </p>
            )}
            <label>
              Token
              <input value={token} onChange={(e) => setToken(e.target.value)} required />
            </label>
            <label>
              Nova senha (mín. 8, maiúscula, minúscula e número)
              <PasswordInput
                value={novaSenha}
                onChange={(e) => setNovaSenha(e.target.value)}
                required
                minLength={8}
                pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}"
                title="Mínimo 8 caracteres com maiúscula, minúscula e número"
                autoComplete="new-password"
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
            <Link className="btn" to={origem.path}>Ir para login</Link>
          </div>
        )}

        <div className="auth-toggle">
          <Link to={origem.path}>Voltar ao login</Link>
        </div>
      </div>
    </div>
  )
}
