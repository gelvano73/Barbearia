// @ts-nocheck
/**
 * Gestão de pedidos do marketplace da barbearia.
 */
import { useEffect, useState } from 'react'
import { marketplaceApi } from '../services/resources'

/** === Helpers === */
function money(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export default function MarketplacePage() {
  /** === Estado === */
  const [pedidos, setPedidos] = useState([])
  const [error, setError] = useState('')

  /** === Carga e ações === */
  const carregar = () =>
    marketplaceApi.pedidos().then((r) => setPedidos(r.data)).catch(() => setError('Erro ao carregar pedidos'))

  useEffect(() => {
    carregar()
  }, [])

  const status = async (id, s) => {
    await marketplaceApi.atualizarStatus(id, s)
    await carregar()
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Marketplace</h1>
          <p>Pedidos da loja online · vitrine pública em /loja/:barbeariaId</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      {/* === Pedidos === */}
      <div className="panel">
        {pedidos.length === 0 ? (
          <div className="empty">Nenhum pedido. Ative produtos no estoque (preço + marketplace) e venda em /loja/1</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Cliente</th>
                <th>Total</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {pedidos.map((p) => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>{p.clienteNome}<br /><small>{p.clienteTelefone}</small></td>
                  <td>{money(p.total)}</td>
                  <td>{p.status}</td>
                  <td>
                    <select value={p.status} onChange={(e) => status(p.id, e.target.value)}>
                      {['PENDENTE', 'PAGO', 'ENVIADO', 'ENTREGUE', 'CANCELADO'].map((s) => (
                        <option key={s} value={s}>{s}</option>
                      ))}
                    </select>
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
