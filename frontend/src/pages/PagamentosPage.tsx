/**
 * Registro e listagem de pagamentos no painel administrativo.
 * Inclui recibo, cancelamento e emissão de NFS-e por pagamento.
 */
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import Modal from '../components/Modal'
import { EmptyState, LoadingState } from '../components/LoadingEmpty'
import { useAuth } from '../context/AuthContext'
import { temRecurso } from '../data/planos'
import { clientesApi, fiscalApi, pagamentosApi, servicosApi } from '../services/resources'

/** === Constantes e helpers === */
const FORMAS = [
  { value: 'PIX', label: 'PIX' },
  { value: 'CREDITO', label: 'Crédito' },
  { value: 'DEBITO', label: 'Débito' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
]

function hojeISO() {
  return new Date().toISOString().slice(0, 10)
}

function money(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

const empty = {
  valor: '',
  formaPagamento: 'PIX',
  clienteId: '',
  servicoId: '',
  dataPagamento: hojeISO(),
  descricao: '',
  online: false,
}

export default function PagamentosPage() {
  /** === Estado === */
  const { isAdmin, auth } = useAuth()
  const podeOnline = temRecurso(auth?.plano as string | undefined, 'PAGAMENTO_ONLINE')
  const podeNfse = temRecurso(auth?.plano as string | undefined, 'NFSE')
  const [searchParams] = useSearchParams()
  const mpRetorno = searchParams.get('mp')
  const [itens, setItens] = useState([])
  const [clientes, setClientes] = useState([])
  const [servicos, setServicos] = useState([])
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(empty)
  const [filtroData, setFiltroData] = useState(hojeISO)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [mpStatus, setMpStatus] = useState(null)

  /** === Carga de dados === */
  const carregar = async () => {
    const [{ data: pags }, { data: cls }, { data: svs }] = await Promise.all([
      pagamentosApi.listar({ data: filtroData }),
      clientesApi.listar(true),
      servicosApi.listar(true),
    ])
    setItens(pags)
    setClientes(cls)
    setServicos(svs)
  }

  useEffect(() => {
    setLoading(true)
    carregar()
      .catch(() => setError('Não foi possível carregar pagamentos'))
      .finally(() => setLoading(false))
  }, [filtroData])

  useEffect(() => {
    if (!isAdmin) return
    pagamentosApi
      .mercadoPagoStatus()
      .then(({ data }) => setMpStatus(data))
      .catch(() => setMpStatus(null))
  }, [isAdmin])

  const podeRegistrar = useMemo(
    () => clientes.length > 0 && servicos.length > 0,
    [clientes, servicos],
  )

  /** === Ações do formulário === */
  const abrirNovo = () => {
    setForm({ ...empty, dataPagamento: filtroData || hojeISO() })
    setError('')
    setOpen(true)
  }

  const escolherServico = (servicoId) => {
    const s = servicos.find((x) => String(x.id) === String(servicoId))
    setForm((prev) => ({
      ...prev,
      servicoId,
      valor: s ? String(s.preco) : prev.valor,
    }))
  }

  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const payload = {
        valor: Number(form.valor),
        formaPagamento: form.formaPagamento,
        clienteId: Number(form.clienteId),
        servicoId: Number(form.servicoId),
        dataPagamento: form.dataPagamento,
        descricao: form.descricao || undefined,
      }
      if (form.online) {
        const { data } = await pagamentosApi.criarOnline(payload)
        setOpen(false)
        if (data.checkoutUrl) {
          window.open(data.checkoutUrl, '_blank', 'noopener,noreferrer')
        }
      } else {
        await pagamentosApi.criar(payload)
        setOpen(false)
      }
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao registrar pagamento')
    }
  }

  const abrirRecibo = (id) => {
    window.open(pagamentosApi.reciboUrl(id), '_blank', 'noopener,noreferrer')
  }

  /** === Ações da listagem === */
  const cancelar = async (id) => {
    if (!confirm('Cancelar este pagamento?')) return
    await pagamentosApi.cancelar(id)
    await carregar()
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Pagamentos</h1>
          <p>PIX, crédito, débito e dinheiro</p>
        </div>
        <button className="btn" type="button" onClick={abrirNovo} disabled={!podeRegistrar}>
          Registrar pagamento
        </button>
      </div>

      {mpRetorno === 'success' && (
        <p className="success" style={{ marginBottom: '0.8rem' }}>
          Retorno Mercado Pago: sucesso. Aguarde a confirmação do webhook para marcar como pago.
        </p>
      )}
      {mpRetorno === 'failure' && (
        <p className="error" style={{ marginBottom: '0.8rem' }}>Pagamento Mercado Pago não concluído.</p>
      )}
      {mpRetorno === 'pending' && (
        <p className="subtitle" style={{ marginBottom: '0.8rem' }}>
          Pagamento pendente (ex.: Pix). O status atualiza quando o webhook confirmar.
        </p>
      )}
      {mpStatus && (
        <div className="panel" style={{ marginBottom: '1rem' }}>
          <p style={{ margin: 0 }}>
            Mercado Pago: {mpStatus.configurado ? 'configurado' : 'não configurado'}
            {mpStatus.ambienteProvavel ? ` · ${mpStatus.ambienteProvavel}` : ''}
            {mpStatus.allowSimulated ? ' · simulado ligado' : ''}
          </p>
          {mpStatus.aviso && <p className="subtitle" style={{ margin: '0.4rem 0 0' }}>{mpStatus.aviso}</p>}
        </div>
      )}

      {loading && <LoadingState label="Carregando pagamentos…" />}

      {/* === Filtros === */}
      <div className="panel" style={{ marginBottom: '1rem' }}>
        <label style={{ maxWidth: 260 }}>
          Data
          <input type="date" value={filtroData} onChange={(e) => setFiltroData(e.target.value)} />
        </label>
        {!podeRegistrar && (
          <p className="empty">Cadastre cliente e serviço para registrar pagamentos.</p>
        )}
      </div>

      {/* === Tabela de pagamentos === */}
      <div className="panel">
        {!loading && itens.length === 0 ? (
          <EmptyState>Nenhum pagamento nesta data.</EmptyState>
        ) : !loading && (
          <table className="table">
            <thead>
              <tr>
                <th>Data</th>
                <th>Cliente</th>
                <th>Serviço</th>
                <th>Forma</th>
                <th>Valor</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>{item.dataPagamento}</td>
                  <td>{item.clienteNome || '—'}</td>
                  <td>{item.servicoNome || '—'}</td>
                  <td>{item.formaPagamento}</td>
                  <td>{money(item.valor)}</td>
                  <td>
                    <span className={`badge ${item.status === 'PAGO' ? 'ok' : 'danger'}`}>{item.status}</span>
                  </td>
                  <td>
                    {item.status !== 'CANCELADO' && (
                      <div className="actions-row">
                        <button className="btn secondary small" type="button" onClick={() => abrirRecibo(item.id)}>
                          Recibo
                        </button>
                        {item.status === 'PAGO' && podeNfse && (
                          <button
                            className="btn secondary small"
                            type="button"
                            onClick={async () => {
                              try {
                                setError('')
                                const { data } = await fiscalApi.emitir(item.id)
                                alert(
                                  data.status === 'AUTORIZADA' || data.numero
                                    ? `NFS-e ${data.numero || data.status}`
                                    : `NFS-e: ${data.status}${data.mensagemErro ? ' — ' + data.mensagemErro : ''}`,
                                )
                              } catch (err) {
                                setError(err.response?.data?.mensagem || 'Falha ao emitir NFS-e')
                              }
                            }}
                          >
                            NFS-e
                          </button>
                        )}
                        <button className="btn danger small" type="button" onClick={() => cancelar(item.id)}>
                          Cancelar
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* === Modal de registro === */}
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
              Serviço
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
                Nenhum serviço cadastrado. Cadastre corte, barba etc. em Serviços.
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
              <input
                value={form.descricao}
                onChange={(e) => setForm({ ...form, descricao: e.target.value })}
                placeholder="Opcional"
              />
            </label>
            {podeOnline && (
            <label className="full aceite-privacidade" style={{ cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={Boolean(form.online)}
                onChange={(e) => setForm({ ...form, online: e.target.checked })}
              />
              <span>Pagamento online (Mercado Pago / PIX link) — gera checkout e deixa status pendente até confirmar</span>
            </label>
            )}
            {!podeOnline && (
              <p className="subtitle" style={{ gridColumn: '1 / -1', margin: 0 }}>
                Pagamento online disponível a partir do plano Pro. Veja em Assinatura.
              </p>
            )}
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn secondary" type="button" onClick={() => setOpen(false)}>Cancelar</button>
            <button className="btn" type="submit">Confirmar</button>
          </div>
        </form>
      </Modal>
    </>
  )
}
