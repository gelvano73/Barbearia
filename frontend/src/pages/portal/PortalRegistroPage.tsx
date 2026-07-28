/**
 * Cadastro de novo cliente no portal da barbearia.
 */
import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import AceitePrivacidade from '../../components/AceitePrivacidade'
import { useAuth } from '../../context/AuthContext'
import { authApi } from '../../services/resources'

export default function PortalRegistroPage() {
  const { isAuthenticated, isCliente, registroCliente } = useAuth()
  const [barbearias, setBarbearias] = useState([])
  const [aceitePrivacidade, setAceitePrivacidade] = useState(false)
  const [form, setForm] = useState({
    barbeariaId: '',
    nome: '',
    telefone: '',
    email: '',
    senha: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [carregandoLista, setCarregandoLista] = useState(true)

  // Effect: carga inicial dos dados
  useEffect(() => {
    setCarregandoLista(true)
    authApi
      .listarBarbearias()
      .then(({ data }) => {
        setBarbearias(data)
        if (data.length) {
          setForm((f) => ({ ...f, barbeariaId: String(data[0].id) }))
          setError('')
        } else {
          setError(
            'Nenhuma barbearia cadastrada ainda. Crie a conta da barbearia em /auth (Registrar barbearia) e volte aqui.'
          )
        }
      })
      .catch(() =>
        setError('Não foi possível carregar barbearias. Verifique se a API está no ar (porta 8080).')
      )
      .finally(() => setCarregandoLista(false))
  }, [])

  if (isAuthenticated && isCliente) return <Navigate to="/portal" replace />

  // Atualiza campos do formulário
  const onChange = (e) => setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  // Submete o formulário
  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!barbearias.length) {
      setError(
        'Nenhuma barbearia disponível. Registre a barbearia em /auth antes de criar conta de cliente.'
      )
      return
    }
    if (!form.barbeariaId) {
      setError('Selecione a barbearia.')
      return
    }
    if (form.senha.length < 6) {
      setError('A senha deve ter no mínimo 6 caracteres.')
      return
    }
    if (!aceitePrivacidade) {
      setError('Aceite a Política de Privacidade para continuar.')
      return
    }

    setLoading(true)
    try {
      await registroCliente({
        ...form,
        barbeariaId: Number(form.barbeariaId),
        aceitePrivacidade: true,
      })
    } catch (err) {
      const detalhes = err.response?.data?.detalhes
      setError(
        (Array.isArray(detalhes) && detalhes.length ? detalhes.join(' · ') : null) ||
          err.response?.data?.mensagem ||
          (err.code === 'ERR_NETWORK' ? 'API offline. Suba o backend na porta 8080.' : null) ||
          'Falha no cadastro'
      )
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
        <h1>Cadastro do cliente</h1>
        <p className="subtitle">Crie sua conta para agendar online.</p>

        <form onSubmit={onSubmit}>
          <label>
            Barbearia
            <select
              name="barbeariaId"
              value={form.barbeariaId}
              onChange={onChange}
              required
              disabled={carregandoLista || !barbearias.length}
            >
              <option value="">
                {carregandoLista
                  ? 'Carregando...'
                  : barbearias.length
                    ? 'Selecione'
                    : 'Nenhuma barbearia'}
              </option>
              {barbearias.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.nome}
                </option>
              ))}
            </select>
          </label>
          <label>
            Nome
            <input name="nome" value={form.nome} onChange={onChange} required />
          </label>
          <label>
            Telefone
            <input name="telefone" value={form.telefone} onChange={onChange} required />
          </label>
          <label>
            Email
            <input type="email" name="email" value={form.email} onChange={onChange} required />
          </label>
          <label>
            Senha (mín. 6)
            <input
              type="password"
              name="senha"
              value={form.senha}
              onChange={onChange}
              required
              minLength={6}
            />
          </label>
          <AceitePrivacidade checked={aceitePrivacidade} onChange={setAceitePrivacidade} />
          {error && <div className="error">{error}</div>}
          <button
            className="btn"
            type="submit"
            disabled={loading || carregandoLista || !barbearias.length || !aceitePrivacidade}
          >
            {loading ? 'Aguarde...' : 'Criar conta'}
          </button>
        </form>

        <div className="auth-toggle">
          Já tem conta? <Link to="/portal/login">Entrar</Link>
          <div style={{ marginTop: '0.5rem' }}>
            <Link to="/auth">Sou dono — cadastrar barbearia</Link>
          </div>
        </div>
      </div>
    </div>
  )
}
