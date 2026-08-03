// @ts-nocheck
/**
 * CRUD do catálogo de serviços (preço, duração e comissão).
 */
import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { servicosApi } from '../services/resources'

/** === Estado inicial === */
const empty = {
  nome: '',
  descricao: '',
  preco: '',
  duracaoMinutos: 30,
  comissaoPercentual: 40,
}

/** === Helpers === */
function formatMoney(value) {
  return Number(value).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export default function ServicosPage() {
  /** === Estado === */
  const [itens, setItens] = useState([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(empty)
  const [error, setError] = useState('')

  /** === Carga de dados === */
  const carregar = async () => {
    const { data } = await servicosApi.listar(true)
    setItens(data)
  }

  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar serviços'))
  }, [])

  /** === Ações CRUD === */
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
      descricao: item.descricao || '',
      preco: item.preco ?? '',
      duracaoMinutos: item.duracaoMinutos ?? 30,
      comissaoPercentual: item.comissaoPercentual ?? 0,
    })
    setError('')
    setOpen(true)
  }

  const payload = () => ({
    nome: form.nome.trim(),
    descricao: form.descricao.trim() || null,
    preco: Number(form.preco),
    duracaoMinutos: Number(form.duracaoMinutos),
    comissaoPercentual: Number(form.comissaoPercentual),
  })

  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    try {
      if (editing) {
        await servicosApi.atualizar(editing.id, payload())
      } else {
        await servicosApi.criar(payload())
      }
      setOpen(false)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao salvar serviço')
    }
  }

  const desativar = async (id) => {
    if (!confirm('Desativar este serviço?')) return
    await servicosApi.desativar(id)
    await carregar()
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Serviços</h1>
          <p>Catálogo de serviços oferecidos pela barbearia</p>
        </div>
        <button className="btn" type="button" onClick={abrirNovo}>
          Novo serviço
        </button>
      </div>

      {/* === Tabela de serviços === */}
      <div className="panel">
        {itens.length === 0 ? (
          <div className="empty">Nenhum serviço cadastrado.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Preço</th>
                <th>Duração</th>
                <th>Comissão</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>
                    <div>{item.nome}</div>
                    {item.descricao && (
                      <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>{item.descricao}</div>
                    )}
                  </td>
                  <td>{formatMoney(item.preco)}</td>
                  <td>{item.duracaoMinutos} min</td>
                  <td>{Number(item.comissaoPercentual)}%</td>
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

      {/* === Modal === */}
      <Modal open={open} title={editing ? 'Editar serviço' : 'Novo serviço'} onClose={() => setOpen(false)}>
        <form onSubmit={salvar}>
          <div className="form-grid">
            <label className="full">
              Nome
              <input
                value={form.nome}
                onChange={(e) => setForm({ ...form, nome: e.target.value })}
                placeholder="Ex.: Corte Masculino"
                required
              />
            </label>
            <label className="full">
              Descrição
              <textarea
                rows={2}
                value={form.descricao}
                onChange={(e) => setForm({ ...form, descricao: e.target.value })}
                placeholder="Opcional"
              />
            </label>
            <label>
              Preço (R$)
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={form.preco}
                onChange={(e) => setForm({ ...form, preco: e.target.value })}
                required
              />
            </label>
            <label>
              Duração (min)
              <input
                type="number"
                min={5}
                max={480}
                value={form.duracaoMinutos}
                onChange={(e) => setForm({ ...form, duracaoMinutos: e.target.value })}
                required
              />
            </label>
            <label>
              Comissão (%)
              <input
                type="number"
                min={0}
                max={100}
                step="0.01"
                value={form.comissaoPercentual}
                onChange={(e) => setForm({ ...form, comissaoPercentual: e.target.value })}
                required
              />
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
