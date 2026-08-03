/**
 * Agenda diária do barbeiro com atualização de status dos atendimentos.
 */
import { useEffect, useState } from 'react'
import { barbeiroPortalApi } from '../../services/resources'

/** === Helpers === */
function formatDateTime(value) {
  return new Date(value).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit',
  })
}

export default function BarbeiroAgendaPage() {
  /** === Estado === */
  const [data, setData] = useState(new Date().toISOString().slice(0, 10))
  const [itens, setItens] = useState([])
  const [error, setError] = useState('')

  /** === Carga e ações === */
  const carregar = async () => {
    const { data: lista } = await barbeiroPortalApi.agenda(data)
    setItens(lista)
  }

  useEffect(() => {
    carregar().catch(() => setError('Falha ao carregar agenda'))
  }, [data])

  const status = async (id, novo) => {
    await barbeiroPortalApi.atualizarStatus(id, novo)
    await carregar()
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Agenda</h1>
          <p>Seus atendimentos do dia</p>
        </div>
        <label>
          Data
          <input type="date" value={data} onChange={(e) => setData(e.target.value)} />
        </label>
      </div>
      {/* === Lista do dia === */}
      <div className="panel">
        {error && <div className="error">{error}</div>}
        {itens.length === 0 ? (
          <div className="empty">Nenhum agendamento neste dia.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Horário</th>
                <th>Cliente</th>
                <th>Serviço</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>{formatDateTime(item.dataHora)}</td>
                  <td>{item.clienteNome}</td>
                  <td>{item.servico || '—'}</td>
                  <td><span className="badge warn">{item.status}</span></td>
                  <td>
                    <div className="actions-row">
                      {item.status !== 'CONFIRMADO' && item.status !== 'CANCELADO' && item.status !== 'CONCLUIDO' && (
                        <button className="btn secondary small" type="button" onClick={() => status(item.id, 'CONFIRMADO')}>Confirmar</button>
                      )}
                      {item.status !== 'CONCLUIDO' && item.status !== 'CANCELADO' && (
                        <button className="btn small" type="button" onClick={() => status(item.id, 'CONCLUIDO')}>Concluir</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
