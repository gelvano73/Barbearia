// @ts-nocheck
/**
 * CRUD de clientes no painel administrativo.
 * Lista, cria, edita, desativa e envia foto de perfil.
 */
import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import FotoField from '../components/FotoField'
import { clientesApi } from '../services/resources'

const empty = { nome: '', telefone: '', email: '', observacoes: '' }

export default function ClientesPage() {
  const [itens, setItens] = useState([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(empty)
  const [fotoUrl, setFotoUrl] = useState('')
  const [error, setError] = useState('')

  // Carrega a listagem principal da página
  const carregar = async () => {
    const { data } = await clientesApi.listar(true)
    setItens(data)
  }

  // Effect: carga inicial dos dados
  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar clientes'))
  }, [])

  // Abre o modal para novo cadastro
  const abrirNovo = () => {
    setEditing(null)
    setForm(empty)
    setFotoUrl('')
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
    setFotoUrl(item.fotoUrl || '')
    setError('')
    setOpen(true)
  }

  // Salva criação ou edição do formulário
  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    try {
      if (editing) {
        await clientesApi.atualizar(editing.id, form)
      } else {
        await clientesApi.criar(form)
      }
      setOpen(false)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao salvar cliente')
    }
  }

  // Envia a foto do cadastro
  const enviarFoto = async (file) => {
    if (!editing?.id) return
    setError('')
    try {
      const { data } = await clientesApi.uploadFoto(editing.id, file)
      setFotoUrl(data.fotoUrl || '')
      setEditing(data)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao enviar foto')
    }
  }

  // Desativa o registro selecionado
  const desativar = async (id) => {
    if (!confirm('Desativar este cliente?')) return
    await clientesApi.desativar(id)
    await carregar()
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Clientes</h1>
          <p>Cadastro e manutenção da base de clientes</p>
        </div>
        <button className="btn" type="button" onClick={abrirNovo}>
          Novo cliente
        </button>
      </div>

      <div className="panel">
        {itens.length === 0 ? (
          <div className="empty">Nenhum cliente cadastrado.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Foto</th>
                <th>Nome</th>
                <th>Telefone</th>
                <th>Email</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>
                    <div className="foto-avatar sm">
                      {item.fotoUrl ? <img src={item.fotoUrl} alt={item.nome} /> : <span>—</span>}
                    </div>
                  </td>
                  <td>{item.nome}</td>
                  <td>{item.telefone}</td>
                  <td>{item.email || '—'}</td>
                  <td>
                    <div className="actions-row">
                      <button className="btn secondary small" type="button" onClick={() => abrirEdicao(item)}>
                        Editar
                      </button>
                      <button className="btn danger small" type="button" onClick={() => desativar(item.id)}>
                        Desativar
                      </button>
                    </div>
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
            <div className="full">
              <FotoField
                fotoUrl={fotoUrl}
                onUpload={enviarFoto}
                disabled={!editing}
              />
            </div>
            <label className="full">
              Nome
              <input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required />
            </label>
            <label>
              Telefone
              <input value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} required />
            </label>
            <label>
              Email
              <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </label>
            <label className="full">
              Observações
              <textarea rows={3} value={form.observacoes} onChange={(e) => setForm({ ...form, observacoes: e.target.value })} />
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn secondary" type="button" onClick={() => setOpen(false)}>
              Cancelar
            </button>
            <button className="btn" type="submit">
              Salvar
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
