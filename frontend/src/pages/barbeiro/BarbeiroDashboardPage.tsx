/**
 * Dashboard do barbeiro com resumo do dia e indicadores.
 */
import { useEffect, useState } from 'react'
import { barbeiroPortalApi } from '../../services/resources'

export default function BarbeiroDashboardPage() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  // Effect: carga inicial dos dados
  useEffect(() => {
    barbeiroPortalApi.dashboard()
      .then((res) => setData(res.data))
      .catch(() => setError('Falha ao carregar dashboard'))
  }, [])

  if (error) return <div className="error">{error}</div>
  if (!data) return <div className="empty">Carregando...</div>

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Olá, {data.nome?.split(' ')[0]}</h1>
          <p>Seu painel pessoal do dia</p>
        </div>
      </div>
      <div className="panel" style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))' }}>
        <div>
          <div className="subtitle">Agenda hoje</div>
          <strong style={{ fontSize: '1.8rem' }}>{data.agendamentosHoje}</strong>
        </div>
        <div>
          <div className="subtitle">Atendimentos no mês</div>
          <strong style={{ fontSize: '1.8rem' }}>{data.atendimentosMes}</strong>
        </div>
        <div>
          <div className="subtitle">Comissão no mês</div>
          <strong style={{ fontSize: '1.8rem' }}>R$ {Number(data.comissaoMes || 0).toFixed(2)}</strong>
        </div>
        <div>
          <div className="subtitle">Média avaliações</div>
          <strong style={{ fontSize: '1.8rem' }}>{Number(data.mediaAvaliacoes || 0).toFixed(1)}</strong>
          <div className="subtitle">{data.totalAvaliacoes} avaliações</div>
        </div>
      </div>

      {data.meta && (
        <div className="panel" style={{ marginTop: '1rem' }}>
          <h2 style={{ marginTop: 0, fontFamily: 'var(--font-display)', letterSpacing: '0.04em' }}>Meta do mês</h2>
          <p style={{ color: 'var(--muted)' }}>
            Atendimentos: {data.meta.atendimentosRealizados}/{data.meta.metaAtendimentos} ({data.meta.percentualAtendimentos}%)
          </p>
          <p style={{ color: 'var(--muted)' }}>
            Comissão: R$ {Number(data.meta.comissaoRealizada || 0).toFixed(2)} / R$ {Number(data.meta.metaComissao || 0).toFixed(2)} ({data.meta.percentualComissao}%)
          </p>
        </div>
      )}

      <div className="panel" style={{ marginTop: '1rem' }}>
        <h2 style={{ marginTop: 0, fontFamily: 'var(--font-display)', letterSpacing: '0.04em' }}>Próximos horários</h2>
        {data.proximosHorarios?.length ? (
          <ul>
            {data.proximosHorarios.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        ) : (
          <div className="empty">Nenhum horário próximo.</div>
        )}
      </div>
    </>
  )
}
