// @ts-nocheck
/**
 * Gestão de agendamentos no painel admin.
 * Cria horários, consulta disponibilidade e atualiza status.
 */
import { useEffect, useMemo, useState } from 'react'
import Modal from '../components/Modal'
import { EmptyState, LoadingState } from '../components/LoadingEmpty'
import { agendamentosApi, barbeirosApi, clientesApi, servicosApi } from '../services/resources'

/** === Estado inicial === */
const empty = {
  clienteId: '',
  barbeiroId: '',
  servicoId: '',
  data: '',
  dataHora: '',
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

function hojeISO() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export default function AgendamentosPage() {
  /** === Estado === */
  const [itens, setItens] = useState([])
  const [clientes, setClientes] = useState([])
  const [barbeiros, setBarbeiros] = useState([])
  const [servicos, setServicos] = useState([])
  const [slots, setSlots] = useState([])
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(empty)
  const [filtroData, setFiltroData] = useState('')
  const [error, setError] = useState('')
  const [loadingSlots, setLoadingSlots] = useState(false)
  const [loading, setLoading] = useState(true)

  /** === Carga de dados === */
  const carregar = async () => {
    const params = {}
    if (filtroData) params.data = filtroData
    const [{ data: ags }, { data: cls }, { data: brs }, { data: svs }] = await Promise.all([
      agendamentosApi.listar(params),
      clientesApi.listar(true),
      barbeirosApi.listar(true),
      servicosApi.listar(true),
    ])
    setItens(ags)
    setClientes(cls)
    setBarbeiros(brs)
    setServicos(svs)
  }

  useEffect(() => {
    setLoading(true)
    carregar()
      .catch(() => setError('Não foi possível carregar agendamentos'))
      .finally(() => setLoading(false))
  }, [filtroData])

  const podeAgendar = useMemo(
    () => clientes.length > 0 && barbeiros.length > 0 && servicos.length > 0,
    [clientes, barbeiros, servicos],
  )

  const servicoSelecionado = useMemo(
    () => servicos.find((s) => String(s.id) === String(form.servicoId)),
    [servicos, form.servicoId],
  )

  const slotSelecionado = useMemo(
    () => slots.find((s) => s.dataHora === form.dataHora),
    [slots, form.dataHora],
  )

  /** === Horários livres === */
  const carregarSlots = async (next = form) => {
    if (!next.barbeiroId || !next.servicoId || !next.data) {
      setSlots([])
      return
    }
    setLoadingSlots(true)
    setError('')
    try {
      const { data } = await agendamentosApi.horariosDisponiveis({
        barbeiroId: Number(next.barbeiroId),
        servicoId: Number(next.servicoId),
        data: next.data,
        limite: 48,
      })
      setSlots(data)
      if (next.dataHora && !data.some((s) => s.dataHora === next.dataHora)) {
        setForm((prev) => ({ ...prev, dataHora: '' }))
      }
    } catch (err) {
      setSlots([])
      setError(err.response?.data?.mensagem || 'Não foi possível carregar horários')
    } finally {
      setLoadingSlots(false)
    }
  }

  const atualizarForm = (patch) => {
    const next = { ...form, ...patch }
    if ('barbeiroId' in patch || 'servicoId' in patch || 'data' in patch) {
      next.dataHora = ''
    }
    setForm(next)
    carregarSlots(next)
  }

  /** === Ações === */
  const abrirNovo = () => {
    const initial = { ...empty, data: hojeISO() }
    setForm(initial)
    setSlots([])
    setError('')
    setOpen(true)
  }

  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    if (!form.dataHora) {
      setError('Escolha um horário disponível')
      return
    }
    try {
      await agendamentosApi.criar({
        clienteId: Number(form.clienteId),
        barbeiroId: Number(form.barbeiroId),
        servicoId: Number(form.servicoId),
        dataHora: form.dataHora.length === 16 ? `${form.dataHora}:00` : form.dataHora,
        observacoes: form.observacoes || undefined,
      })
      setOpen(false)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao criar agendamento')
    }
  }

  const atualizarStatus = async (id, status) => {
    await agendamentosApi.atualizarStatus(id, status)
    await carregar()
  }

  const cancelar = async (id) => {
    if (!confirm('Cancelar este agendamento?')) return
    await agendamentosApi.cancelar(id)
    await carregar()
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Agendamentos</h1>
          <p>Agenda inteligente: barbeiro, serviço, horários livres e confirmação</p>
        </div>
        <button className="btn" type="button" onClick={abrirNovo} disabled={!podeAgendar}>
          Novo agendamento
        </button>
      </div>

      {/* === Filtros === */}
      <div className="panel" style={{ marginBottom: '1rem' }}>
        <label style={{ maxWidth: 260 }}>
          Filtrar por data
          <input type="date" value={filtroData} onChange={(e) => setFiltroData(e.target.value)} />
        </label>
        {!podeAgendar && (
          <p className="empty">Cadastre cliente, barbeiro e serviço para agendar.</p>
        )}
      </div>

      {/* === Tabela === */}
      <div className="panel">
        {loading ? (
          <LoadingState label="Carregando agendamentos…" />
        ) : itens.length === 0 ? (
          <EmptyState>Nenhum agendamento encontrado.</EmptyState>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Data/Hora</th>
                <th>Cliente</th>
                <th>Barbeiro</th>
                <th>Serviço</th>
                <th>Duração</th>
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
                  <td>{item.duracaoMinutos} min</td>
                  <td>
                    <span className={`badge ${statusClass(item.status)}`}>{item.status}</span>
                  </td>
                  <td>
                    <div className="actions-row">
                      {item.status !== 'CONFIRMADO' && item.status !== 'CANCELADO' && (
                        <button className="btn secondary small" type="button" onClick={() => atualizarStatus(item.id, 'CONFIRMADO')}>
                          Confirmar
                        </button>
                      )}
                      {item.status !== 'CONCLUIDO' && item.status !== 'CANCELADO' && (
                        <button className="btn secondary small" type="button" onClick={() => atualizarStatus(item.id, 'CONCLUIDO')}>
                          Concluir
                        </button>
                      )}
                      {item.status !== 'CANCELADO' && item.status !== 'CONCLUIDO' && (
                        <button className="btn danger small" type="button" onClick={() => cancelar(item.id)}>
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

      {/* === Modal agendamento === */}
      <Modal open={open} title="Agenda inteligente" onClose={() => setOpen(false)}>
        <form onSubmit={salvar}>
          <div className="form-grid">
            <label>
              Cliente
              <select value={form.clienteId} onChange={(e) => setForm({ ...form, clienteId: e.target.value })} required>
                <option value="">Selecione</option>
                {clientes.map((c) => (
                  <option key={c.id} value={c.id}>{c.nome}</option>
                ))}
              </select>
            </label>
            <label>
              Barbeiro
              <select
                value={form.barbeiroId}
                onChange={(e) => atualizarForm({ barbeiroId: e.target.value })}
                required
              >
                <option value="">Selecione</option>
                {barbeiros.map((b) => (
                  <option key={b.id} value={b.id}>{b.nome}</option>
                ))}
              </select>
            </label>
            <label className="full">
              Serviço
              <select
                value={form.servicoId}
                onChange={(e) => atualizarForm({ servicoId: e.target.value })}
                required
              >
                <option value="">Selecione</option>
                {servicos.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nome} — R$ {Number(s.preco).toFixed(2)} ({s.duracaoMinutos} min)
                  </option>
                ))}
              </select>
            </label>
            <label>
              Data
              <input
                type="date"
                min={hojeISO()}
                value={form.data}
                onChange={(e) => atualizarForm({ data: e.target.value })}
                required
              />
            </label>
            <label>
              Duração
              <input
                value={servicoSelecionado ? `${servicoSelecionado.duracaoMinutos} min (automático)` : 'Escolha o serviço'}
                readOnly
              />
            </label>
            <div className="full">
              <div style={{ marginBottom: '0.45rem', color: 'var(--muted)', fontSize: '0.9rem' }}>
                Horários disponíveis
              </div>
              {!form.barbeiroId || !form.servicoId || !form.data ? (
                <p className="empty" style={{ margin: 0 }}>Escolha barbeiro, serviço e data.</p>
              ) : loadingSlots ? (
                <p className="empty" style={{ margin: 0 }}>Buscando horários livres...</p>
              ) : slots.length === 0 ? (
                <p className="empty" style={{ margin: 0 }}>Nenhum horário livre neste dia.</p>
              ) : (
                <div className="slot-grid">
                  {slots.map((slot) => (
                    <button
                      key={slot.dataHora}
                      type="button"
                      className={`slot-btn ${form.dataHora === slot.dataHora ? 'active' : ''}`}
                      onClick={() => setForm({ ...form, dataHora: slot.dataHora })}
                    >
                      {new Date(slot.dataHora).toLocaleTimeString('pt-BR', {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </button>
                  ))}
                </div>
              )}
            </div>
            {form.dataHora && servicoSelecionado && (
              <div className="full confirm-box">
                <strong>Confirmar horário</strong>
                <p>
                  {servicoSelecionado.nome} com{' '}
                  {slotSelecionado?.barbeiroNome
                    || barbeiros.find((b) => String(b.id) === String(form.barbeiroId))?.nome}
                  {' · '}
                  {formatDateTime(form.dataHora)}
                  {' · '}
                  {servicoSelecionado.duracaoMinutos} min
                </p>
              </div>
            )}
            <label className="full">
              Observações
              <textarea
                rows={2}
                value={form.observacoes}
                onChange={(e) => setForm({ ...form, observacoes: e.target.value })}
              />
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn secondary" type="button" onClick={() => setOpen(false)}>
              Cancelar
            </button>
            <button className="btn" type="submit" disabled={!form.dataHora}>
              Confirmar horário
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
