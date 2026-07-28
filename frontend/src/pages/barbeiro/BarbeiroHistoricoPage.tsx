/**
 * Histórico de atendimentos realizados pelo barbeiro.
 */
import { useEffect, useState } from 'react'
import { barbeiroPortalApi } from '../../services/resources'

function formatDateTime(value) {
  return new Date(value).toLocaleString('pt-BR')
}

export default function BarbeiroHistoricoPage() {
  const [itens, setItens] = useState([])

  // Effect: carga inicial dos dados
  useEffect(() => {
    barbeiroPortalApi.historico().then(({ data }) => setItens(data))
  }, [])

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Histórico</h1>
          <p>Atendimentos concluídos</p>
        </div>
      </div>
      <div className="panel">
        {itens.length === 0 ? (
          <div className="empty">Nenhum atendimento concluído.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Data</th>
                <th>Cliente</th>
                <th>Serviço</th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>{formatDateTime(item.dataHora)}</td>
                  <td>{item.clienteNome}</td>
                  <td>{item.servico || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
