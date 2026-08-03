// @ts-nocheck
/**
 * Exibe previsões e insights de gestão gerados por IA.
 */
import { useEffect, useState } from 'react'
import { gestaoApi } from '../services/resources'

/** === Helpers === */
function money(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export default function GestaoIaPage() {
  /** === Estado === */
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  /** === Carga de dados === */
  useEffect(() => {
    gestaoApi
      .previsoes()
      .then((r) => setData(r.data))
      .catch(() => setError('Não foi possível carregar as previsões'))
  }, [])

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>IA de Gestão</h1>
          <p>Previsão de faturamento e estoque</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      {data && (
        <>
          {/* === Insights === */}
          <div className="panel" style={{ marginBottom: '1rem' }}>
            <p style={{ margin: 0 }}>{data.insight}</p>
            <div className="actions-row" style={{ marginTop: '1rem', flexWrap: 'wrap', gap: '1.5rem' }}>
              <div>
                <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Últimos 30 dias</div>
                <strong>{money(data.faturamentoUltimos30Dias)}</strong>
              </div>
              <div>
                <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Previsto 7 dias</div>
                <strong style={{ fontSize: '1.2rem' }}>{money(data.previstoProximos7Dias)}</strong>
              </div>
              <div>
                <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Previsto 30 dias</div>
                <strong>{money(data.previstoProximos30Dias)}</strong>
              </div>
            </div>
          </div>
          {/* === Faturamento previsto === */}
          <div className="panel" style={{ marginBottom: '1rem' }}>
            <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Faturamento previsto</h2>
            <table className="table">
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Previsto</th>
                </tr>
              </thead>
              <tbody>
                {data.faturamentoPorDia?.map((d) => (
                  <tr key={d.data}>
                    <td>{d.data}</td>
                    <td>{money(d.previsto)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {/* === Previsão de estoque === */}
          <div className="panel">
            <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Previsão de estoque</h2>
            <table className="table">
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Estoque</th>
                  <th>Consumo/dia</th>
                  <th>Dias restantes</th>
                  <th>Risco</th>
                </tr>
              </thead>
              <tbody>
                {data.estoque?.map((e) => (
                  <tr key={e.produtoId}>
                    <td>{e.produtoNome}</td>
                    <td>{Number(e.estoqueAtual).toFixed(1)}</td>
                    <td>{Number(e.consumoMedioDiario).toFixed(2)}</td>
                    <td>{e.diasRestantes ?? '—'}</td>
                    <td>{e.risco}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </>
  )
}
