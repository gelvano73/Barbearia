/**
 * Registro de pagamentos pela recepção.
 */
import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { recepcaoApi } from '../../services/resources'

/** === Constantes === */
const FORMAS = [
  { value: 'PIX', label: 'PIX' },
  { value: 'CREDITO', label: 'Crédito' },
  { value: 'DEBITO', label: 'Débito' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
]

/** === Estado inicial === */
const empty = {
  valor: '',
  formaPagamento: 'PIX',
  clienteId: '',
  servicoId: '',
  dataPagamento: '',
  agendamentoId: '',
  descricao: '',
}

/** === Helpers === */
function money(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatDateTime(value) {
  return new Date(value).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function RecepcaoPagamentosPage() {
  /** === Estado === */
  const [itens, setItens] = useState([])
  const [clientes, setClientes] = useState([])
  const [servicos, setServicos] = useState([])
  const [agendamentos, setAgendamentos] = useState([])
  const [caixa, setCaixa] = useState(null)
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(empty)
  const [data, setData] = useState(() => new Date().toISOString().slice(0, 10))
  const [error, setError] = useState('')

  /** === Carga de dados === */
  const carregar = async () => {
    const [{ data: pags }, { data: cls }, { data: svs }, { data: ags }, { data: cx }] = await Promise.all([
      recepcaoApi.pagamentos({ data }),
      recepcaoApi.clientes(),
      recepcaoApi.servicos(),
      recepcaoApi.agendamentos({ data }),
      recepcaoApi.caixaAtual(),
    ])
    setItens(pags)
    setClientes(cls)
    setServicos(svs)
    setAgendamentos(ags.filter((a) => !['CANCELADO'].includes(a.status)))
    setCaixa(cx)
  }

  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar pagamentos'))
  }, [data])

  /** === Ações === */
  const abrirNovo = () => {
    setForm({ ...empty, dataPagamento: data })
    setError('')
    setOpen(true)
  }

  const nomeServicoDoAgendamento = (a) => {
    if (!a) return ''
    if (a.servico) return a.servico
    const s = servicos.find((x) => String(x.id) === String(a.servicoId))
    return s?.nome || ''
  }

  const escolherServico = (servicoId) => {
    const s = servicos.find((x) => String(x.id) === String(servicoId))
    setForm((prev) => ({
      ...prev,
      servicoId,
      valor: s ? String(s.preco) : prev.valor,
    }))
  }

  const escolherAgendamento = (agendamentoId) => {
    const a = agendamentos.find((x) => String(x.id) === String(agendamentoId))
    if (!a) {
      setForm((prev) => ({ ...prev, agendamentoId: '' }))
      return
    }
    const porId = a.servicoId
      ? servicos.find((s) => String(s.id) === String(a.servicoId))
      : null
    const porNome = !porId && a.servico
      ? servicos.find((s) => s.nome?.toLowerCase() === String(a.servico).toLowerCase())
      : null
    const servico = porId || porNome
    setForm((prev) => ({
      ...prev,
      agendamentoId,
      clienteId: a.clienteId ? String(a.clienteId) : prev.clienteId,
      servicoId: servico ? String(servico.id) : prev.servicoId,
      valor: servico ? String(servico.preco) : prev.valor,
    }))
  }

  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await recepcaoApi.criarPagamento({
        valor: Number(form.valor),
        formaPagamento: form.formaPagamento,
        clienteId: Number(form.clienteId),
        servicoId: Number(form.servicoId),
        dataPagamento: form.dataPagamento,
        agendamentoId: form.agendamentoId ? Number(form.agendamentoId) : null,
        descricao: form.descricao || undefined,
      })
      setOpen(false)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao registrar pagamento')
    }
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Pagamentos</h1>
          <p>Recebimentos do dia {caixa ? '· caixa aberto' : '· abra o caixa antes de receber'}</p>
        </div>
        <button className="btn" type="button" onClick={abrirNovo} disabled={!caixa}>
          Registrar pagamento
        </button>
      </div>

      {/* === Filtro === */}
      <div className="panel" style={{ marginBottom: '1rem' }}>
        <label>
          Data
          <input type="date" value={data} onChange={(e) => setData(e.target.value)} />
        </label>
      </div>

      {!caixa && (
        <div className="error" style={{ marginBottom: '1rem' }}>
          Não há caixa aberto. Vá em Caixa e abra o dia.
        </div>
      )}

      {/* === Lista === */}
      <div className="panel">
        {itens.length === 0 ? (
          <div className="empty">Nenhum pagamento nesta data.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Data</th>
                <th>Cliente</th>
                <th>Serviço</th>
                <th>Forma</th>
                <th>Valor</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>{item.dataPagamento || formatDateTime(item.criadoEm)}</td>
                  <td>{item.clienteNome || '—'}</td>
                  <td>{item.servicoNome || '—'}</td>
                  <td>{item.formaPagamento}</td>
                  <td>{money(item.valor)}</td>
                  <td>
                    <span className={`badge ${item.status === 'PAGO' ? 'ok' : 'danger'}`}>{item.status}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* === Modal === */}
      <Modal open={open} title="Registrar pagamento" onClose={() => setOpen(false)}>
        <form onSubmit={salvar}>
          <div className="form-grid">
            <label>
              Data
              <input
                type="date"
                value={form.dataPagamento}
                onChange={(e) => setForm({ ...form, dataPagamento: e.target.value })}
                required
              />
            </label>
            <label>
              Forma
              <select
                value={form.formaPagamento}
                onChange={(e) => setForm({ ...form, formaPagamento: e.target.value })}
                required
              >
                {FORMAS.map((f) => (
                  <option key={f.value} value={f.value}>{f.label}</option>
                ))}
              </select>
            </label>
            <label>
              Cliente
              <select
                value={form.clienteId}
                onChange={(e) => setForm({ ...form, clienteId: e.target.value })}
                required
              >
                <option value="">Selecione</option>
                {clientes.map((c) => (
                  <option key={c.id} value={c.id}>{c.nome}</option>
                ))}
              </select>
            </label>
            <label>
              Agendamento (opcional)
              <select
                value={form.agendamentoId}
                onChange={(e) => escolherAgendamento(e.target.value)}
              >
                <option value="">—</option>
                {agendamentos.map((a) => (
                  <option key={a.id} value={a.id}>
                    {nomeServicoDoAgendamento(a) || 'Serviço'} · {a.clienteNome} · {formatDateTime(a.dataHora)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Tipo de serviço
              <select
                value={form.servicoId}
                onChange={(e) => escolherServico(e.target.value)}
                required
              >
                <option value="">Selecione</option>
                {servicos.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nome} — {money(s.preco)}
                  </option>
                ))}
              </select>
            </label>
            {servicos.length === 0 && (
              <p className="error full" style={{ margin: 0 }}>
                Nenhum serviço cadastrado. Cadastre o tipo (corte, barba…) em Serviços no painel admin.
              </p>
            )}
            <label>
              Valor
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={form.valor}
                onChange={(e) => setForm({ ...form, valor: e.target.value })}
                required
              />
            </label>
            <label className="full">
              Descrição
              <input value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} />
            </label>
          </div>
          {error && <div className="error">{error}</div>}
          <div className="actions-row" style={{ marginTop: '1rem' }}>
            <button className="btn" type="submit">
              Confirmar
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
