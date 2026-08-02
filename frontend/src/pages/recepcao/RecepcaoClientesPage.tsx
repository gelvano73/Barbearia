/**
 * Cadastro e edição rápida de clientes pela recepção.
 */
import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { recepcaoApi } from '../../services/resources'
import { emailRealOk, MSG_EMAIL_INVALIDO } from '../../utils/email'

const empty = { nome: '', telefone: '', email: '', cpf: '', observacoes: '' }

export default function RecepcaoClientesPage() {
  const [itens, setItens] = useState([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(empty)
  const [error, setError] = useState('')
  const [filtro, setFiltro] = useState('')

  const carregar = async () => {
    const { data } = await recepcaoApi.clientes()
    setItens(data)
  }

  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar clientes'))
  }, [])

  const filtrados = itens.filter((c) => {
    const q = filtro.trim().toLowerCase()
    if (!q) return true
    return (
      c.nome?.toLowerCase().includes(q) ||
      c.telefone?.includes(q) ||
      c.cpf?.includes(q) ||
      c.email?.toLowerCase().includes(q)
    )
  })

  const abrirNovo = () => {
    setEditing(null)
    setForm(empty)
    setError('')
    setOpen(true)
  }

  const abrirEdicao = (item) => {
    setEditing(item)
    setForm({
      nome: item.nome || '',
      telefone: item.telefone || '',
      email: item.email || '',
      cpf: item.cpf || '',
      observacoes: item.observacoes || '',
    })
    setError('')
    setOpen(true)
  }

  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    if (form.email?.trim() && !emailRealOk(form.email)) {
      setError(MSG_EMAIL_INVALIDO)
      return
    }
    if (!editing && !form.cpf?.replace(/\D/g, '').match(/^\d{11}$/)) {
      setError('Informe o CPF real do cliente (11 dígitos) para NFS-e.')
      return
    }
    try {
      if (editing) {
        await recepcaoApi.atualizarCliente(editing.id, form)
      } else {
        await recepcaoApi.criarCliente(form)
      }
      setOpen(false)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao salvar cliente')
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Clientes</h1>
          <p>Cadastro rápido na recepção</p>
        </div>
        <button className="btn" type="button" onClick={abrirNovo}>
          Novo cliente
        </button>
      </div>

      <div className="panel">
        <input
          placeholder="Nome, telefone, CPF ou email"
          value={filtro}
          onChange={(e) => setFiltro(e.target.value)}
          style={{ marginBottom: '1rem', maxWidth: 360 }}
        />
        {error && <div className="error">{error}</div>}
        {filtrados.length === 0 ? (
          <p className="empty">Nenhum cliente.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>CPF</th>
                <th>Telefone</th>
                <th>Email</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {filtrados.map((item) => (
                <tr key={item.id}>
                  <td>{item.nome}</td>
                  <td>{item.cpf || '—'}</td>
                  <td>{item.telefone}</td>
                  <td>{item.email || '—'}</td>
                  <td>
                    <button className="btn secondary small" type="button" onClick={() => abrirEdicao(item)}>
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <Modal open={open} title={editing ? 'Editar cliente' : 'Novo cliente'} onClose={() => setOpen(false)}>
        <form onSubmit={salvar}>
          <div className="form-grid">
            <label className="full">
              Nome
              <input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required />
            </label>
            <label>
              Telefone
              <input
                value={form.telefone}
                onChange={(e) => setForm({ ...form, telefone: e.target.value })}
                required
              />
            </label>
            <label>
              CPF (Receita Federal)
              <input
                value={form.cpf}
                onChange={(e) => setForm({ ...form, cpf: e.target.value })}
                required={!editing}
                placeholder="000.000.000-00"
                inputMode="numeric"
              />
            </label>
            <label>
              Email (real)
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </label>
            <label className="full">
              Observações
              <textarea
                value={form.observacoes}
                onChange={(e) => setForm({ ...form, observacoes: e.target.value })}
                rows={3}
              />
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="actions-row" style={{ marginTop: '1rem' }}>
            <button className="btn" type="submit">
              Salvar
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
