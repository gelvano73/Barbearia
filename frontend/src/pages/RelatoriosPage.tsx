/**
 * Geração de relatórios gerenciais com filtros de período.
 */
import { useEffect, useState } from 'react'
import { relatoriosApi } from '../services/resources'

function money(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatDate(value) {
  if (!value) return '—'
  const [y, m, d] = String(value).split('-')
  return `${d}/${m}/${y}`
}

const PERIODOS = [
  { value: 'DIARIO', label: 'Diário' },
  { value: 'SEMANAL', label: 'Semanal' },
  { value: 'MENSAL', label: 'Mensal' },
]

export default function RelatoriosPage() {
  const today = new Date().toISOString().slice(0, 10)
  const [periodo, setPeriodo] = useState('MENSAL')
  const [data, setData] = useState(today)
  const [relatorio, setRelatorio] = useState(null)
  const [error, setError] = useState('')

  // Effect: carga inicial dos dados
  useEffect(() => {
    setError('')
    relatoriosApi
      .gerar({ periodo, data })
      .then(({ data: payload }) => setRelatorio(payload))
      .catch(() => setError('Não foi possível carregar o relatório'))
  }, [periodo, data])

  const lucro = relatorio?.lucroLiquido

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Relatórios</h1>
          <p>
            {relatorio
              ? `${formatDate(relatorio.inicio)} a ${formatDate(relatorio.fim)}`
              : 'Faturamento, rankings e lucro líquido'}
          </p>
        </div>
        <div className="actions-row">
          <label>
            Período
            <select value={periodo} onChange={(e) => setPeriodo(e.target.value)}>
              {PERIODOS.map((p) => (
                <option key={p.value} value={p.value}>{p.label}</option>
              ))}
            </select>
          </label>
          <label>
            Referência
            <input type="date" value={data} onChange={(e) => setData(e.target.value)} />
          </label>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      {relatorio && (
        <>
          <div className="panel" style={{ marginBottom: '1rem' }}>
            <div className="actions-row" style={{ flexWrap: 'wrap', gap: '1.5rem' }}>
              <div>
                <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Faturamento</div>
                <strong style={{ fontSize: '1.25rem' }}>{money(relatorio.faturamentoTotal)}</strong>
              </div>
              <div>
                <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Pagamentos</div>
                <strong>{relatorio.quantidadePagamentos}</strong>
              </div>
              <div>
                <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Comissões</div>
                <strong>{money(lucro?.comissoes)}</strong>
              </div>
              <div>
                <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Lucro líquido</div>
                <strong style={{ fontSize: '1.25rem' }}>{money(lucro?.lucroLiquido)}</strong>
              </div>
            </div>
            <p style={{ margin: '0.75rem 0 0', color: 'var(--muted)', fontSize: '0.85rem' }}>
              Lucro líquido = faturamento (pagamentos) − comissões do período.
            </p>
          </div>

          <div className="panel" style={{ marginBottom: '1rem' }}>
            <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Faturamento por dia</h2>
            {!relatorio.faturamentoPorDia?.length ? (
              <div className="empty">Sem faturamento neste período.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Pagamentos</th>
                    <th>Total</th>
                  </tr>
                </thead>
                <tbody>
                  {relatorio.faturamentoPorDia.map((item) => (
                    <tr key={item.data}>
                      <td>{formatDate(item.data)}</td>
                      <td>{item.quantidade}</td>
                      <td>{money(item.total)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          <div className="panel" style={{ marginBottom: '1rem' }}>
            <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Serviços mais vendidos</h2>
            {!relatorio.servicosMaisVendidos?.length ? (
              <div className="empty">Sem vendas de serviços neste período.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Serviço</th>
                    <th>Qtd</th>
                    <th>Total</th>
                  </tr>
                </thead>
                <tbody>
                  {relatorio.servicosMaisVendidos.map((item) => (
                    <tr key={item.servicoId}>
                      <td>{item.posicao}º</td>
                      <td>{item.servicoNome}</td>
                      <td>{item.quantidade}</td>
                      <td>{money(item.total)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          <div className="panel">
            <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Clientes mais frequentes</h2>
            {!relatorio.clientesMaisFrequentes?.length ? (
              <div className="empty">Sem clientes neste período.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Cliente</th>
                    <th>Visitas</th>
                    <th>Total gasto</th>
                  </tr>
                </thead>
                <tbody>
                  {relatorio.clientesMaisFrequentes.map((item) => (
                    <tr key={item.clienteId}>
                      <td>{item.posicao}º</td>
                      <td>{item.clienteNome}</td>
                      <td>{item.frequencia}</td>
                      <td>{money(item.totalGasto)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </>
  )
}
