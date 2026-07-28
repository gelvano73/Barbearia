/**
 * Lista as avaliações recebidas pelos clientes.
 */
import { useEffect, useState } from 'react'
import { barbeiroPortalApi } from '../../services/resources'

export default function BarbeiroAvaliacoesPage() {
  const [itens, setItens] = useState([])

  // Effect: carga inicial dos dados
  useEffect(() => {
    barbeiroPortalApi.avaliacoes().then(({ data }) => setItens(data))
  }, [])

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Avaliações</h1>
          <p>Feedback recebido dos clientes</p>
        </div>
      </div>
      <div className="panel">
        {itens.length === 0 ? (
          <div className="empty">Nenhuma avaliação ainda.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Nota</th>
                <th>Comentário</th>
                <th>Data</th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>{item.nota}/5</td>
                  <td>{item.comentario || '—'}</td>
                  <td>{item.criadoEm ? new Date(item.criadoEm).toLocaleString('pt-BR') : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
