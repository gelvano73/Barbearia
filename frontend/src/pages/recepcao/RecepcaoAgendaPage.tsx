// @ts-nocheck
/**
 * Agenda da recepção: criação de horários e mudança de status.
 */
import { useEffect, useMemo, useState } from 'react'
import Modal from '../../components/Modal'
import { recepcaoApi } from '../../services/resources'

/** === Estado inicial === */
const empty = {
  clienteId: '',
  barbeiroId: '',
  dataHora: '',
  duracaoMinutos: 30,
  servico: '',
  observacoes: '',
}

/** === Helpers === */
function statusClass(status) {
  if (['CONCLUIDO', 'CONFIRMADO'].includes(status)) return 'ok'
  if (['CANCELADO', 'NAO_COMPARECEU'].includes(status)) return 'danger'
  return 'warn'
}

function formatDateTime(value) {
  return new Date(value).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function RecepcaoAgendaPage() {
  /** === Estado === */
  const [itens, setItens] = useState([])
  const [clientes, setClientes] = useState([])
  const [barbeiros, setBarbeiros] = useState([])
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(empty)
  const [filtroData, setFiltroData] = useState(() => new Date().toISOString().slice(0, 10))
  const [error, setError] = useState('')

  /** === Carga de dados === */
  const carregar = async () => {
    const params = {}
    if (filtroData) params.data = filtroData
    const [{ data: ags }, { data: cls }, { data: brs }] = await Promise.all([
      recepcaoApi.agendamentos(params),
      recepcaoApi.clientes(),
      recepcaoApi.barbeiros(),
    ])
    setItens(ags)
    setClientes(cls)
    setBarbeiros(brs)
  }

  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar a agenda'))
  }, [filtroData])

  const podeAgendar = useMemo(() => clientes.length > 0 && barbeiros.length > 0, [clientes, barbeiros])

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
      const dataHora = form.dataHora.length === 16 ? `${form.dataHora}:00` : form.dataHora
      await recepcaoApi.criarAgendamento({
        ...form,
        dataHora,
        clienteId: Number(form.clienteId),
        barbeiroId: Number(form.barbeiroId),
        duracaoMinutos: Number(form.duracaoMinutos),
      })
      setOpen(false)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao criar agendamento')
    }
  }

  const atualizarStatus = async (id, status) => {
    await recepcaoApi.atualizarStatusAgendamento(id, status)
    await carregar()
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Agendamento manual</h1>
          <p>Marque horários pelo balcão</p>
        </div>
        <button className="btn" type="button" onClick={abrirNovo} disabled={!podeAgendar}>
          Novo agendamento
        </button>
      </div>

      {/* === Filtro === */}
      <div className="panel" style={{ marginBottom: '1rem' }}>
        <label>
          Data
          <input type="date" value={filtroData} onChange={(e) => setFiltroData(e.target.value)} />
        </label>
      </div>

      {!podeAgendar && (
        <div className="error" style={{ marginBottom: '1rem' }}>
          Cadastre pelo menos um cliente e um barbeiro para agendar.
        </div>
      )}

      {/* === Lista === */}
      <div className="panel">
        {itens.length === 0 ? (
          <div className="empty">Nenhum agendamento nesta data.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Horário</th>
                <th>Cliente</th>
                <th>Barbeiro</th>
                <th>Serviço</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>{formatDateTime(item.dataHora)}</td>
                  <td>{item.clienteNome}</td>
                  <td>{item.barbeiroNome}</td>
                  <td>{item.servico || '—'}</td>
                  <td>
                    <span className={`badge ${statusClass(item.status)}`}>{item.status}</span>
                  </td>
                  <td>
                    <div className="actions-row">
                      {item.status === 'AGENDADO' && (
                        <button
                          className="btn secondary small"
                          type="button"
                          onClick={() => atualizarStatus(item.id, 'CONFIRMADO')}
                        >
                          Confirmar
                        </button>
                      )}
                      {['AGENDADO', 'CONFIRMADO'].includes(item.status) && (
                        <button
                          className="btn secondary small"
                          type="button"
                          onClick={() => atualizarStatus(item.id, 'CONCLUIDO')}
                        >
                          Concluir
                        </button>
                      )}
                      {!['CANCELADO', 'CONCLUIDO'].includes(item.status) && (
                        <button
                          className="btn danger small"
                          type="button"
                          onClick={() => atualizarStatus(item.id, 'CANCELADO')}
                        >
                          Cancelar
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
      <Modal open={open} title="Novo agendamento" onClose={() => setOpen(false)}>
        <form onSubmit={salvar}>
          <div className="form-grid">
            <label>
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
              Barbeiro
              <select
                value={form.barbeiroId}
                onChange={(e) => setForm({ ...form, barbeiroId: e.target.value })}
                required
              >
                <option value="">Selecione</option>
                {barbeiros.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.nome}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Data e hora
              <input
                type="datetime-local"
                value={form.dataHora}
                onChange={(e) => setForm({ ...form, dataHora: e.target.value })}
                required
              />
            </label>
            <label>
              Duração (min)
              <input
                type="number"
                min={15}
                step={15}
                value={form.duracaoMinutos}
                onChange={(e) => setForm({ ...form, duracaoMinutos: e.target.value })}
              />
            </label>
            <label className="full">
              Serviço
              <input value={form.servico} onChange={(e) => setForm({ ...form, servico: e.target.value })} />
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
              Agendar
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
