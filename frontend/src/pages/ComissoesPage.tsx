/**
 * Consulta de comissões dos barbeiros (detalhe e consolidado mensal).
 */
import { useEffect, useState } from 'react'
import { barbeirosApi, comissoesApi } from '../services/resources'

/** === Helpers === */
function money(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

/** === Constantes === */
const MESES = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
]

export default function ComissoesPage() {
  /** === Estado === */
  const now = new Date()
  const [ano, setAno] = useState(now.getFullYear())
  const [mes, setMes] = useState(now.getMonth() + 1)
  const [barbeiroId, setBarbeiroId] = useState('')
  const [barbeiros, setBarbeiros] = useState([])
  const [resumo, setResumo] = useState(null)
  const [detalhes, setDetalhes] = useState([])
  const [error, setError] = useState('')

  /** === Carga de dados === */
  const carregar = async () => {
    setError('')
    const params = { ano, mes }
    const [mensal, lista] = await Promise.all([
      comissoesApi.mensal(params),
      comissoesApi.listar({
        ...params,
        ...(barbeiroId ? { barbeiroId } : {}),
      }),
    ])
    setResumo(mensal.data)
    setDetalhes(lista.data)
  }

  useEffect(() => {
    barbeirosApi.listar(true).then(({ data }) => setBarbeiros(data)).catch(() => {})
  }, [])

  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar as comissões'))
  }, [ano, mes, barbeiroId])

  return (
    <>
      {/* === Cabeçalho e filtros === */}
      <div className="page-header">
        <div>
          <h1>Comissões</h1>
          <p>
            {MESES[mes - 1]} de {ano} — cálculo automático ao concluir atendimento
          </p>
        </div>
        <div className="actions-row">
          <label>
            Mês
            <select value={mes} onChange={(e) => setMes(Number(e.target.value))}>
              {MESES.map((nome, i) => (
                <option key={nome} value={i + 1}>{nome}</option>
              ))}
            </select>
          </label>
          <label>
            Ano
            <input type="number" value={ano} onChange={(e) => setAno(Number(e.target.value))} />
          </label>
          <label>
            Barbeiro
            <select value={barbeiroId} onChange={(e) => setBarbeiroId(e.target.value)}>
              <option value="">Todos</option>
              {barbeiros.map((b) => (
                <option key={b.id} value={b.id}>{b.nome}</option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      {/* === Resumo === */}
      {resumo && (
        <div className="panel" style={{ marginBottom: '1rem' }}>
          <div className="actions-row" style={{ flexWrap: 'wrap', gap: '1.5rem' }}>
            <div>
              <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Comissão mensal</div>
              <strong style={{ fontSize: '1.25rem' }}>{money(resumo.totalComissoes)}</strong>
            </div>
            <div>
              <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Total serviços</div>
              <strong>{money(resumo.totalServicos)}</strong>
            </div>
            <div>
              <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Atendimentos</div>
              <strong>{resumo.totalAtendimentos}</strong>
            </div>
          </div>
        </div>
      )}

      {/* === Ranking === */}
      <div className="panel" style={{ marginBottom: '1rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Ranking do mês</h2>
        {!resumo?.ranking?.length ? (
          <div className="empty">Sem comissões neste período.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Barbeiro</th>
                <th>Atendimentos</th>
                <th>Serviços</th>
                <th>Comissão</th>
              </tr>
            </thead>
            <tbody>
              {resumo.ranking.map((item) => (
                <tr key={item.barbeiroId}>
                  <td>{item.posicao}º</td>
                  <td>{item.barbeiroNome}</td>
                  <td>{item.atendimentos}</td>
                  <td>{money(item.totalServicos)}</td>
                  <td>{money(item.totalComissao)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* === Detalhe === */}
      <div className="panel">
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>
          {barbeiroId ? 'Comissões do barbeiro' : 'Detalhe por atendimento'}
        </h2>
        {detalhes.length === 0 ? (
          <div className="empty">Nenhum lançamento neste filtro.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Data</th>
                <th>Barbeiro</th>
                <th>Cliente</th>
                <th>Serviço</th>
                <th>Valor</th>
                <th>%</th>
                <th>Comissão</th>
              </tr>
            </thead>
            <tbody>
              {detalhes.map((item) => (
                <tr key={item.id}>
                  <td>
                    {item.criadoEm
                      ? new Date(item.criadoEm).toLocaleString('pt-BR', {
                          day: '2-digit',
                          month: '2-digit',
                          hour: '2-digit',
                          minute: '2-digit',
                        })
                      : '—'}
                  </td>
                  <td>{item.barbeiroNome}</td>
                  <td>{item.clienteNome}</td>
                  <td>{item.servico || '—'}</td>
                  <td>{money(item.valorServico)}</td>
                  <td>{Number(item.percentual).toFixed(0)}%</td>
                  <td>{money(item.valorComissao)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
