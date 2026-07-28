/**
 * Registro e listagem de pagamentos no painel administrativo.
 */
import { useEffect, useMemo, useState } from 'react'
import Modal from '../components/Modal'
import { clientesApi, pagamentosApi, servicosApi } from '../services/resources'

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
}

export default function PagamentosPage() {
  const [itens, setItens] = useState([])
  const [clientes, setClientes] = useState([])
  const [servicos, setServicos] = useState([])
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(empty)
  const [filtroData, setFiltroData] = useState(hojeISO)
  const [error, setError] = useState('')

  // Carrega a listagem principal da página
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

  // Effect: carga inicial dos dados
  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar pagamentos'))
  }, [filtroData])

  const podeRegistrar = useMemo(
    () => clientes.length > 0 && servicos.length > 0,
    [clientes, servicos],
  )

  // Abre o modal para novo cadastro
  const abrirNovo = () => {
    setForm({ ...empty, dataPagamento: filtroData || hojeISO() })
    setError('')
    setOpen(true)
  }

  // Seleciona serviço sugerido pela IA
  const escolherServico = (servicoId) => {
    const s = servicos.find((x) => String(x.id) === String(servicoId))
    setForm((prev) => ({
      ...prev,
      servicoId,
      valor: s ? String(s.preco) : prev.valor,
    }))
  }

  // Salva criação ou edição do formulário
  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await pagamentosApi.criar({
        valor: Number(form.valor),
        formaPagamento: form.formaPagamento,
        clienteId: Number(form.clienteId),
        servicoId: Number(form.servicoId),
        dataPagamento: form.dataPagamento,
        descricao: form.descricao || undefined,
      })
      setOpen(false)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao registrar pagamento')
    }
  }

  // Cancela o item selecionado
  const cancelar = async (id) => {
    if (!confirm('Cancelar este pagamento?')) return
    await pagamentosApi.cancelar(id)
    await carregar()
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Pagamentos</h1>
          <p>PIX, crédito, débito e dinheiro</p>
        </div>
        <button className="btn" type="button" onClick={abrirNovo} disabled={!podeRegistrar}>
          Registrar pagamento
        </button>
      </div>

      <div className="panel" style={{ marginBottom: '1rem' }}>
        <label style={{ maxWidth: 260 }}>
          Data
          <input type="date" value={filtroData} onChange={(e) => setFiltroData(e.target.value)} />
        </label>
        {!podeRegistrar && (
          <p className="empty">Cadastre cliente e serviço para registrar pagamentos.</p>
        )}
      </div>

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
                      <button className="btn danger small" type="button" onClick={() => cancelar(item.id)}>
                        Cancelar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

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
