/**
 * Dashboard inicial do admin com atalhos para os módulos.
 * Hero com imagem de fundo de barbearia e criação de recepcionista.
 */
import { useState } from 'react'
import { Link } from 'react-router-dom'
import Modal from '../components/Modal'
import { useAuth } from '../context/AuthContext'
import { authApi } from '../services/resources'

const emptyAtendente = { nome: '', email: '', senha: '' }

const atalhos = [
  { to: '/clientes', label: 'Clientes', primary: true },
  { to: '/barbeiros', label: 'Barbeiros' },
  { to: '/servicos', label: 'Serviços' },
  { to: '/agendamentos', label: 'Agendamentos' },
  { to: '/pagamentos', label: 'Pagamentos' },
  { to: '/fidelidade', label: 'Fidelidade' },
  { to: '/estoque', label: 'Estoque' },
  { to: '/caixa', label: 'Caixa' },
  { to: '/comissoes', label: 'Comissões' },
  { to: '/relatorios', label: 'Relatórios' },
  { to: '/unidades', label: 'Unidades' },
  { to: '/whatsapp', label: 'WhatsApp IA' },
  { to: '/gestao', label: 'IA Gestão' },
  { to: '/checkin', label: 'Check-in' },
  { to: '/marketplace', label: 'Marketplace' },
  { to: '/franquias', label: 'Franquias' },
  { to: '/recepcao', label: 'Portal recepção' },
]

export default function DashboardPage() {
  const { auth } = useAuth()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(emptyAtendente)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')

  // Cria conta de recepcionista
  const criarAtendente = async (e) => {
    e.preventDefault()
    setError('')
    setOk('')
    try {
      await authApi.criarAtendente({
        nome: form.nome.trim(),
        email: form.email.trim(),
        senha: form.senha,
      })
      setOk(`Atendente criado. Login em /recepcao/login com ${form.email.trim()}`)
      setForm(emptyAtendente)
      setOpen(false)
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao criar atendente')
    }
  }

  return (
    <>
      <section className="dashboard-hero">
        <div className="dashboard-hero-bg" aria-hidden="true" />
        <div className="dashboard-hero-content">
          <div className="page-header dashboard-hero-header">
            <div>
              <p className="dashboard-kicker">{auth?.nomeBarbearia || 'Barbearia'}</p>
              <h1>Olá, {auth?.nome?.split(' ')[0]}</h1>
              <p>Comece pelos cadastros, monte a agenda e acompanhe o dia pelo painel.</p>
            </div>
            <button
              className="btn secondary"
              type="button"
              onClick={() => {
                setError('')
                setOpen(true)
              }}
            >
              Criar recepcionista
            </button>
          </div>

          {ok && <p className="dashboard-ok">{ok}</p>}

          <div className="dashboard-shortcuts">
            {atalhos.map((item) => (
              <Link
                key={item.to}
                className={`dashboard-chip ${item.primary ? 'primary' : ''}`}
                to={item.to}
              >
                {item.label}
              </Link>
            ))}
          </div>
        </div>
      </section>

      <Modal open={open} title="Criar conta de recepcionista" onClose={() => setOpen(false)}>
        <form onSubmit={criarAtendente}>
          <div className="form-grid">
            <label className="full">
              Nome
              <input
                value={form.nome}
                onChange={(e) => setForm({ ...form, nome: e.target.value })}
                required
              />
            </label>
            <label className="full">
              Email
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </label>
            <label className="full">
              Senha
              <input
                type="password"
                value={form.senha}
                onChange={(e) => setForm({ ...form, senha: e.target.value })}
                required
                minLength={6}
              />
            </label>
          </div>
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" style={{ marginTop: '1rem' }}>
            Criar
          </button>
        </form>
      </Modal>
    </>
  )
}
