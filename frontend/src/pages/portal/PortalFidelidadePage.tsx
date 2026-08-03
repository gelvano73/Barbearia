/**
 * Consulta saldo e histórico de pontos de fidelidade do cliente.
 */
import { useEffect, useState } from 'react'
import { portalApi } from '../../services/resources'

/** === Helpers === */
function formatDateTime(value) {
  return new Date(value).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function PortalFidelidadePage() {
  /** === Estado === */
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  /** === Carga de dados === */
  useEffect(() => {
    portalApi.fidelidade()
      .then(({ data: d }) => setData(d))
      .catch(() => setError('Não foi possível carregar fidelidade'))
  }, [])

  if (error) {
    return <div className="error">{error}</div>
  }

  if (!data) {
    return <div className="empty">Carregando...</div>
  }

  const { config, saldo, historico } = data
  const faltam = Math.max(0, (config?.pontosParaResgate || 10) - (saldo?.pontos || 0))

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Fidelidade</h1>
          <p>{config?.descricao || 'Acumule pontos e ganhe cortes grátis'}</p>
        </div>
      </div>

      {/* === Saldo === */}
      <div className="panel confirm-box" style={{ marginBottom: '1rem' }}>
        <strong>{saldo?.pontos ?? 0} pontos</strong>
        <p>
          {saldo?.podeResgatar
            ? 'Você já pode resgatar 1 corte grátis — fale na recepção.'
            : `Faltam ${faltam} ponto(s) para o próximo grátis.`}
        </p>
        <p style={{ marginTop: '0.5rem' }}>
          Acumulados: {saldo?.pontosAcumulados ?? 0} · Resgates: {saldo?.resgates ?? 0}
        </p>
      </div>

      {/* === Histórico === */}
      <div className="panel">
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Histórico</h2>
        {!historico?.length ? (
          <div className="empty">Sem movimentos ainda. Pontos entram ao concluir um atendimento.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Data</th>
                <th>Tipo</th>
                <th>Pontos</th>
                <th>Saldo</th>
                <th>Descrição</th>
              </tr>
            </thead>
            <tbody>
              {historico.map((m) => (
                <tr key={m.id}>
                  <td>{formatDateTime(m.criadoEm)}</td>
                  <td>{m.tipo}</td>
                  <td>{m.tipo === 'RESGATE' ? `-${m.pontos}` : `+${m.pontos}`}</td>
                  <td>{m.saldoApos}</td>
                  <td>{m.descricao || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
