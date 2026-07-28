// @ts-nocheck
/**
 * Loja pública do marketplace (sem login) para listar produtos e criar pedidos.
 */
import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import AceitePrivacidade from '../components/AceitePrivacidade'
import { marketplacePublicApi } from '../services/resources'

function money(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export default function LojaPublicaPage() {
  const { barbeariaId } = useParams()
  const [produtos, setProdutos] = useState([])
  const [cart, setCart] = useState({})
  const [form, setForm] = useState({ clienteNome: '', clienteTelefone: '', clienteEmail: '', enderecoEntrega: '' })
  const [aceitePrivacidade, setAceitePrivacidade] = useState(false)
  const [ok, setOk] = useState('')
  const [error, setError] = useState('')

  // Effect: carga inicial dos dados
  useEffect(() => {
    marketplacePublicApi
      .produtos(barbeariaId)
      .then((r) => setProdutos(r.data))
      .catch(() => setError('Catálogo indisponível'))
  }, [barbeariaId])

  // Adiciona item ao carrinho
  const add = (id) => setCart((c) => ({ ...c, [id]: (c[id] || 0) + 1 }))

  // Finaliza o pedido do carrinho
  const finalizar = async (e) => {
    e.preventDefault()
    setError('')
    setOk('')
    const itens = Object.entries(cart)
      .filter(([, q]) => q > 0)
      .map(([produtoId, quantidade]) => ({ produtoId: Number(produtoId), quantidade }))
    if (!itens.length) {
      setError('Adicione produtos')
      return
    }
    if (!aceitePrivacidade) {
      setError('Aceite a Política de Privacidade para continuar.')
      return
    }
    try {
      const { data } = await marketplacePublicApi.criarPedido(barbeariaId, {
        ...form,
        itens,
        aceitePrivacidade: true,
      })
      setOk(`Pedido #${data.id} criado · total ${money(data.total)}`)
      setCart({})
      setAceitePrivacidade(false)
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao finalizar')
    }
  }

  return (
    <div className="main" style={{ maxWidth: 800, margin: '0 auto', padding: '2rem 1rem' }}>
      <h1>Loja online</h1>
      <p style={{ color: 'var(--muted)' }}>Barbearia #{barbeariaId}</p>
      {error && <div className="error">{error}</div>}
      {ok && <p style={{ color: 'var(--ok)' }}>{ok}</p>}
      <div className="panel" style={{ marginBottom: '1rem' }}>
        {produtos.length === 0 ? (
          <div className="empty">Nenhum produto na vitrine.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Produto</th>
                <th>Preço</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {produtos.map((p) => (
                <tr key={p.id}>
                  <td>
                    <strong>{p.nome}</strong>
                    {p.descricao && <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>{p.descricao}</div>}
                  </td>
                  <td>{money(p.preco)}</td>
                  <td>
                    <button className="btn secondary" type="button" onClick={() => add(p.id)}>
                      + {cart[p.id] || 0}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      <div className="panel">
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Checkout</h2>
        <form onSubmit={finalizar} className="form-grid">
          <label>
            Nome
            <input required value={form.clienteNome} onChange={(e) => setForm({ ...form, clienteNome: e.target.value })} />
          </label>
          <label>
            Telefone
            <input required value={form.clienteTelefone} onChange={(e) => setForm({ ...form, clienteTelefone: e.target.value })} />
          </label>
          <label className="full">
            Endereço
            <input value={form.enderecoEntrega} onChange={(e) => setForm({ ...form, enderecoEntrega: e.target.value })} />
          </label>
          <div className="full">
            <AceitePrivacidade
              id="aceite-loja"
              checked={aceitePrivacidade}
              onChange={setAceitePrivacidade}
            />
          </div>
          <button className="btn" type="submit" disabled={!aceitePrivacidade}>
            Finalizar pedido
          </button>
        </form>
      </div>
    </div>
  )
}
