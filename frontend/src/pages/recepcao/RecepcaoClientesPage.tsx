/**
 * Cadastro e edição rápida de clientes pela recepção.
 */
import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { recepcaoApi } from '../../services/resources'

const empty = { nome: '', telefone: '', email: '', observacoes: '' }

export default function RecepcaoClientesPage() {
  const [itens, setItens] = useState([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(empty)
  const [error, setError] = useState('')
  const [filtro, setFiltro] = useState('')

  // Carrega a listagem principal da página
  const carregar = async () => {
    const { data } = await recepcaoApi.clientes()
    setItens(data)
  }

  // Effect: carga inicial dos dados
  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar clientes'))
  }, [])

  const filtrados = itens.filter((c) => {
    const q = filtro.trim().toLowerCase()
    if (!q) return true
    return (
      c.nome?.toLowerCase().includes(q) ||
      c.telefone?.includes(q) ||
      c.email?.toLowerCase().includes(q)
    )
  })

  // Abre o modal para novo cadastro
  const abrirNovo = () => {
    setEditing(null)
    setForm(empty)
    setError('')
    setOpen(true)
  }

  // Abre o modal preenchido para edição
  const abrirEdicao = (item) => {
    setEditing(item)
    setForm({
      nome: item.nome || '',
      telefone: item.telefone || '',
      email: item.email || '',
      observacoes: item.observacoes || '',
    })
    setError('')
    setOpen(true)
  }

  // Salva criação ou edição do formulário
  const salvar = async (e) => {
    e.preventDefault()
    setError('')
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

      <div className="panel" style={{ marginBottom: '1rem' }}>
        <label>
          Buscar
          <input
            value={filtro}
            onChange={(e) => setFiltro(e.target.value)}
            placeholder="Nome, telefone ou email"
          />
        </label>
      </div>

      <div className="panel">
        {filtrados.length === 0 ? (
          <div className="empty">Nenhum cliente encontrado.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Telefone</th>
                <th>Email</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {filtrados.map((item) => (
                <tr key={item.id}>
                  <td>{item.nome}</td>
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
              Email
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
          {error && <div className="error">{error}</div>}
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
