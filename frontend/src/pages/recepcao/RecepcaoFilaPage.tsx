/**
 * Fila de espera: entrada de clientes e atualização de status.
 */
import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { recepcaoApi } from '../../services/resources'

/** === Estado inicial === */
const empty = {
  clienteId: '',
  barbeiroId: '',
  servicoId: '',
  prioridade: false,
  observacoes: '',
}

/** === Helpers === */
function statusClass(status) {
  if (status === 'EM_ATENDIMENTO') return 'ok'
  if (['CANCELADO', 'DESISTIU', 'FINALIZADO'].includes(status)) return 'danger'
  return 'warn'
}

export default function RecepcaoFilaPage() {
  /** === Estado === */
  const [fila, setFila] = useState([])
  const [clientes, setClientes] = useState([])
  const [barbeiros, setBarbeiros] = useState([])
  const [servicos, setServicos] = useState([])
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(empty)
  const [error, setError] = useState('')

  /** === Carga de dados === */
  const carregar = async () => {
    const [{ data: f }, { data: c }, { data: b }, { data: s }] = await Promise.all([
      recepcaoApi.fila(),
      recepcaoApi.clientes(),
      recepcaoApi.barbeiros(),
      recepcaoApi.servicos(),
    ])
    setFila(f)
    setClientes(c)
    setBarbeiros(b)
    setServicos(s)
  }

  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar a fila'))
    const t = setInterval(() => {
      recepcaoApi.fila().then(({ data }) => setFila(data)).catch(() => {})
    }, 15000)
    return () => clearInterval(t)
  }, [])

  /** === Ações === */
  const abrirNovo = () => {
    setForm(empty)
    setError('')
    setOpen(true)
  }

  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await recepcaoApi.entrarFila({
        clienteId: Number(form.clienteId),
        barbeiroId: form.barbeiroId ? Number(form.barbeiroId) : null,
        servicoId: form.servicoId ? Number(form.servicoId) : null,
        prioridade: form.prioridade,
        observacoes: form.observacoes || undefined,
      })
      setOpen(false)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao adicionar na fila')
    }
  }

  const atualizar = async (id, status) => {
    try {
      await recepcaoApi.atualizarFila(id, status)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao atualizar fila')
    }
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Fila de atendimento</h1>
          <p>Controle de walk-in e prioridade</p>
        </div>
        <button className="btn" type="button" onClick={abrirNovo} disabled={clientes.length === 0}>
          Entrar na fila
        </button>
      </div>

      {error && <div className="error" style={{ marginBottom: '1rem' }}>{error}</div>}

      {/* === Lista da fila === */}
      <div className="panel">
        {fila.length === 0 ? (
          <div className="empty">Fila vazia no momento.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Cliente</th>
                <th>Barbeiro</th>
                <th>Serviço</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {fila.map((item) => (
                <tr key={item.id}>
                  <td>
                    {item.prioridade ? '★ ' : ''}
                    {item.posicao}
                  </td>
                  <td>{item.clienteNome}</td>
                  <td>{item.barbeiroNome || 'Qualquer'}</td>
                  <td>{item.servicoNome || '—'}</td>
                  <td>
                    <span className={`badge ${statusClass(item.status)}`}>{item.status}</span>
                  </td>
                  <td>
                    <div className="actions-row">
                      {item.status === 'AGUARDANDO' && (
                        <button
                          className="btn secondary small"
                          type="button"
                          onClick={() => atualizar(item.id, 'EM_ATENDIMENTO')}
                        >
                          Atender
                        </button>
                      )}
                      {['AGUARDANDO', 'EM_ATENDIMENTO'].includes(item.status) && (
                        <button
                          className="btn small"
                          type="button"
                          onClick={() => atualizar(item.id, 'FINALIZADO')}
                        >
                          Finalizar
                        </button>
                      )}
                      {item.status === 'AGUARDANDO' && (
                        <button
                          className="btn danger small"
                          type="button"
                          onClick={() => atualizar(item.id, 'DESISTIU')}
                        >
                          Desistiu
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* === Modal === */}
      <Modal open={open} title="Adicionar à fila" onClose={() => setOpen(false)}>
        <form onSubmit={salvar}>
          <div className="form-grid">
            <label className="full">
              Cliente
              <select
                value={form.clienteId}
                onChange={(e) => setForm({ ...form, clienteId: e.target.value })}
                required
              >
                <option value="">Selecione</option>
                {clientes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nome}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Barbeiro (opcional)
              <select
                value={form.barbeiroId}
                onChange={(e) => setForm({ ...form, barbeiroId: e.target.value })}
              >
                <option value="">Qualquer</option>
                {barbeiros.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.nome}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Serviço (opcional)
              <select
                value={form.servicoId}
                onChange={(e) => setForm({ ...form, servicoId: e.target.value })}
              >
                <option value="">—</option>
                {servicos.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nome}
                  </option>
                ))}
              </select>
            </label>
            <label className="full" style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem' }}>
              <input
                type="checkbox"
                checked={form.prioridade}
                onChange={(e) => setForm({ ...form, prioridade: e.target.checked })}
              />
              Prioridade
            </label>
            <label className="full">
              Observações
              <textarea
                value={form.observacoes}
                onChange={(e) => setForm({ ...form, observacoes: e.target.value })}
                rows={2}
              />
            </label>
          </div>
          {error && <div className="error">{error}</div>}
          <div className="actions-row" style={{ marginTop: '1rem' }}>
            <button className="btn" type="submit">
              Adicionar
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
