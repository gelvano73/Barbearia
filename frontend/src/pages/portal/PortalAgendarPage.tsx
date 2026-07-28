/**
 * Fluxo de agendamento do cliente: serviço, barbeiro, data e horário.
 */
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { portalApi } from '../../services/resources'

function hojeISO() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export default function PortalAgendarPage() {
  const navigate = useNavigate()
  const [barbeiros, setBarbeiros] = useState([])
  const [servicos, setServicos] = useState([])
  const [slots, setSlots] = useState([])
  const [form, setForm] = useState({ barbeiroId: '', servicoId: '', data: hojeISO(), dataHora: '', observacoes: '' })
  const [error, setError] = useState('')
  const [loadingSlots, setLoadingSlots] = useState(false)

  // Effect: carga inicial dos dados
  useEffect(() => {
    Promise.all([portalApi.barbeiros(), portalApi.servicos()])
      .then(([b, s]) => {
        setBarbeiros(b.data)
        setServicos(s.data)
      })
      .catch(() => setError('Não foi possível carregar catálogo'))
  }, [])

  const servicoSelecionado = useMemo(
    () => servicos.find((s) => String(s.id) === String(form.servicoId)),
    [servicos, form.servicoId],
  )

  // Carrega slots disponíveis
  const carregarSlots = async (next = form) => {
    if (!next.barbeiroId || !next.servicoId || !next.data) {
      setSlots([])
      return
    }
    setLoadingSlots(true)
    try {
      const { data } = await portalApi.horariosDisponiveis({
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

  // Atualiza o formulário e recarrega slots se necessário
  const atualizarForm = (patch) => {
    const next = { ...form, ...patch }
    if ('barbeiroId' in patch || 'servicoId' in patch || 'data' in patch) {
      next.dataHora = ''
    }
    setForm(next)
    setError('')
    carregarSlots(next)
  }

  // Submete o formulário
  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (!form.dataHora) {
      setError('Escolha um horário disponível')
      return
    }
    try {
      await portalApi.agendar({
        barbeiroId: Number(form.barbeiroId),
        servicoId: Number(form.servicoId),
        dataHora: form.dataHora.length === 16 ? `${form.dataHora}:00` : form.dataHora,
        observacoes: form.observacoes || undefined,
      })
      navigate('/portal/agendamentos')
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao agendar')
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Agendar</h1>
          <p>Escolha serviço, barbeiro e um horário livre</p>
        </div>
      </div>
      <div className="panel">
        <form onSubmit={onSubmit}>
          <div className="form-grid">
            <label>
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
              {!form.barbeiroId || !form.servicoId ? (
                <p className="empty" style={{ margin: 0 }}>Escolha serviço e barbeiro.</p>
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
                  {servicoSelecionado.nome} · {formatDateTime(form.dataHora)} · {servicoSelecionado.duracaoMinutos} min
                </p>
              </div>
            )}
            <label className="full">
              Observações
              <textarea
                rows={3}
                value={form.observacoes}
                onChange={(e) => setForm({ ...form, observacoes: e.target.value })}
              />
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn" type="submit" disabled={!barbeiros.length || !form.dataHora}>
              Confirmar agendamento
            </button>
          </div>
        </form>
      </div>
    </>
  )
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
