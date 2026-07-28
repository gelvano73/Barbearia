/**
 * Consulta de comissões do barbeiro autenticado.
 */
import { useEffect, useState } from 'react'
import { barbeiroPortalApi } from '../../services/resources'

export default function BarbeiroComissoesPage() {
  const [itens, setItens] = useState([])
  const now = new Date()
  const [ano, setAno] = useState(now.getFullYear())
  const [mes, setMes] = useState(now.getMonth() + 1)

  // Effect: carga inicial dos dados
  useEffect(() => {
    barbeiroPortalApi.comissoes({ ano, mes }).then(({ data }) => setItens(data))
  }, [ano, mes])

  const total = itens.reduce((acc, i) => acc + Number(i.valorComissao || 0), 0)

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Comissões</h1>
          <p>Cálculo automático ao concluir atendimento</p>
        </div>
        <div className="actions-row">
          <label>
            Mês
            <input type="number" min={1} max={12} value={mes} onChange={(e) => setMes(Number(e.target.value))} />
          </label>
          <label>
            Ano
            <input type="number" value={ano} onChange={(e) => setAno(Number(e.target.value))} />
          </label>
        </div>
      </div>
      <div className="panel">
        <p style={{ color: 'var(--muted)' }}>Total do período: <strong>R$ {total.toFixed(2)}</strong></p>
        {itens.length === 0 ? (
          <div className="empty">Sem comissões neste período.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Cliente</th>
                <th>Serviço</th>
                <th>Valor</th>
                <th>%</th>
                <th>Comissão</th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>{item.clienteNome}</td>
                  <td>{item.servico || '—'}</td>
                  <td>R$ {Number(item.valorServico).toFixed(2)}</td>
                  <td>{Number(item.percentual).toFixed(0)}%</td>
                  <td>R$ {Number(item.valorComissao).toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
