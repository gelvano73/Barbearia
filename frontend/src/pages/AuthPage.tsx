/**
 * Página de login e registro do painel administrativo.
 * Alterna entre modos e redireciona conforme o papel já autenticado.
 */
import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import AceitePrivacidade from '../components/AceitePrivacidade'
import { useAuth } from '../context/AuthContext'

export default function AuthPage() {
  const { isAuthenticated, isCliente, isBarbeiro, isAtendente, login, registro } = useAuth()
  const [mode, setMode] = useState('login')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [aceitePrivacidade, setAceitePrivacidade] = useState(false)
  const [form, setForm] = useState({
    email: '',
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

  // Atualiza campos do formulário
  const onChange = (e) => setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  // Submete o formulário
  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (mode === 'registro' && !aceitePrivacidade) {
      setError('Aceite a Política de Privacidade para continuar.')
      return
    }
    setLoading(true)
    try {
      if (mode === 'login') {
        await login(form.email.trim(), form.senha)
      } else {
        await registro({
          email: form.email.trim(),
          senha: form.senha,
          nomeAdmin: form.nomeAdmin.trim(),
          nomeBarbearia: form.nomeBarbearia.trim(),
          telefoneBarbearia: form.telefoneBarbearia.trim() || undefined,
          cnpj: form.cnpj.replace(/\D/g, '') || undefined,
          aceitePrivacidade: true,
        })
      }
    } catch (err) {
      const detalhes = err.response?.data?.detalhes
      const apiOffline =
        err.code === 'ERR_NETWORK' ||
        err.message === 'Network Error' ||
        err.response?.status === 502 ||
        err.response?.status === 504
      const msg =
        (Array.isArray(detalhes) && detalhes.length ? detalhes.join(' · ') : null) ||
        err.response?.data?.mensagem ||
        err.response?.data?.erro ||
        (err.response?.status === 500
          ? 'Erro no servidor (500). Verifique se a API está no ar na porta 8080.'
          : null) ||
        (apiOffline ? 'API offline. Suba o backend na porta 8080.' : null) ||
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
                Telefone
                <input name="telefoneBarbearia" value={form.telefoneBarbearia} onChange={onChange} />
              </label>
              <label>
                CNPJ
                <input name="cnpj" value={form.cnpj} onChange={onChange} placeholder="00.000.000/0000-00" />
              </label>
            </>
          )}
          <label>
            Email
            <input type="email" name="email" value={form.email} onChange={onChange} required />
          </label>
          <label>
            Senha (mínimo 6 caracteres)
            <input type="password" name="senha" value={form.senha} onChange={onChange} required minLength={6} />
          </label>
          {mode === 'registro' && (
            <AceitePrivacidade checked={aceitePrivacidade} onChange={setAceitePrivacidade} />
          )}
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" disabled={loading || (mode === 'registro' && !aceitePrivacidade)}>
            {loading ? 'Aguarde...' : mode === 'login' ? 'Entrar' : 'Criar conta'}
          </button>
        </form>

        <div className="auth-toggle">
          {mode === 'login' ? (
            <>
              Ainda não tem conta?{' '}
              <button type="button" onClick={() => { setMode('registro'); setAceitePrivacidade(false); setError('') }}>
                Registrar barbearia
              </button>
              <div style={{ marginTop: '0.5rem' }}>
                <a href="/portal/login">Sou cliente — acessar portal</a>
                {' · '}
                <a href="/barbeiro/login">Sou barbeiro</a>
                {' · '}
                <a href="/recepcao/login">Sou recepção</a>
              </div>
              <div style={{ marginTop: '0.65rem' }}>
                <a href="/privacidade">Política de Privacidade</a>
              </div>
            </>
          ) : (
            <>
              Já tem conta?{' '}
              <button type="button" onClick={() => setMode('login')}>
                Fazer login
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
