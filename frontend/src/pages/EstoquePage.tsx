// @ts-nocheck
/**
 * Controle de produtos e movimentações de estoque.
 */
import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { estoqueApi } from '../services/resources'

const emptyProduto = { nome: '', unidade: 'UN', estoqueMinimo: 5 }
const emptyMov = { produtoId: '', tipo: 'ENTRADA', quantidade: '', observacao: '' }

function formatDateTime(value) {
  return new Date(value).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function qty(v) {
  return Number(v || 0).toLocaleString('pt-BR', { maximumFractionDigits: 3 })
}

/** Converte número BR (1.234,56) ou EN (1234.56) para Number válido. */
function parseNumero(value) {
  if (value === null || value === undefined || value === '') return NaN
  if (typeof value === 'number') return value
  let s = String(value).trim().replace(/\s/g, '')
  if (s.includes(',') && s.includes('.')) {
    // 1.234,56 → remove milhar e troca vírgula
    s = s.replace(/\./g, '').replace(',', '.')
  } else if (s.includes(',')) {
    s = s.replace(',', '.')
  }
  return Number(s)
}

export default function EstoquePage() {
  const [produtos, setProdutos] = useState([])
  const [movimentos, setMovimentos] = useState([])
  const [produtoOpen, setProdutoOpen] = useState(false)
  const [movOpen, setMovOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [formProduto, setFormProduto] = useState(emptyProduto)
  const [formMov, setFormMov] = useState(emptyMov)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')

  // Carrega a listagem principal da página
  const carregar = async () => {
    setError('')
    const [prodsRes, movsRes] = await Promise.allSettled([
      estoqueApi.produtos(true),
      estoqueApi.movimentos(),
    ])

    if (prodsRes.status === 'fulfilled') {
      setProdutos(prodsRes.value.data || [])
    } else {
      setProdutos([])
      const err = prodsRes.reason
      const msg =
        err?.response?.data?.mensagem ||
        (err?.code === 'ERR_NETWORK' || !err?.response
          ? 'API offline ou inacessível (porta 8080).'
          : `Falha ao listar produtos (${err?.response?.status || 'erro'})`)
      setError(msg)
      throw err
    }

    if (movsRes.status === 'fulfilled') {
      setMovimentos((movsRes.value.data || []).slice(0, 30))
    } else {
      setMovimentos([])
    }
  }

  // Effect: carga inicial dos dados
  useEffect(() => {
    carregar().catch(() => {
      /* erro já definido em carregar */
    })
  }, [])

  const abrirNovoProduto = () => {
    setEditing(null)
    setFormProduto(emptyProduto)
    setError('')
    setProdutoOpen(true)
  }

  // Abre o modal preenchido para edição
  const abrirEdicao = (p) => {
    setEditing(p)
    setFormProduto({
      nome: p.nome,
      unidade: p.unidade || 'UN',
      estoqueMinimo: p.estoqueMinimo ?? 0,
    })
    setError('')
    setProdutoOpen(true)
  }

  // Salva produto no estoque
  const salvarProduto = async (e) => {
    e.preventDefault()
    setError('')
    const estoqueMinimo = parseNumero(formProduto.estoqueMinimo)
    if (Number.isNaN(estoqueMinimo) || estoqueMinimo < 0) {
      setError('Estoque mínimo inválido. Use números como 5 ou 5.5')
      return
    }
    try {
      // Monta o payload tipado para a API
      const payload = {
        nome: formProduto.nome.trim(),
        unidade: formProduto.unidade.trim() || 'UN',
        estoqueMinimo,
        preco: 0,
        marketplaceAtivo: false,
      }
      if (editing) {
        await estoqueApi.atualizarProduto(editing.id, payload)
      } else {
        await estoqueApi.criarProduto(payload)
      }
      setProdutoOpen(false)
      setOk('Produto salvo')
      await carregar()
    } catch (err) {
      const data = err.response?.data
      const detalhes = data?.detalhes
      const msg =
        (Array.isArray(detalhes) && detalhes.length ? detalhes.join(' · ') : null) ||
        data?.mensagem ||
        data?.message ||
        (err.code === 'ERR_NETWORK' || !err.response
          ? 'API offline. Suba o backend na porta 8080.'
          : null) ||
        `Erro ao salvar produto${err.response?.status ? ` (${err.response.status})` : ''}`
      setError(msg)
    }
  }

  // Desativa o registro selecionado
  const desativar = async (id) => {
    if (!confirm('Desativar este produto?')) return
    await estoqueApi.desativarProduto(id)
    await carregar()
  }

  const abrirMovimento = (tipo, produtoId = '') => {
    setFormMov({ ...emptyMov, tipo, produtoId: produtoId ? String(produtoId) : '' })
    setError('')
    setOk('')
    setMovOpen(true)
  }

  // Registra movimentação de estoque
  const salvarMovimento = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await estoqueApi.movimentar({
        produtoId: Number(formMov.produtoId),
        tipo: formMov.tipo,
        quantidade: Number(formMov.quantidade),
        observacao: formMov.observacao || undefined,
      })
      setMovOpen(false)
      setOk(
        formMov.tipo === 'ENTRADA'
          ? 'Entrada registrada'
          : formMov.tipo === 'SAIDA'
            ? 'Saída registrada'
            : 'Inventário ajustado',
      )
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao registrar movimento')
    }
  }

  const labelMov = formMov.tipo === 'INVENTARIO'
    ? 'Quantidade contada'
    : 'Quantidade'

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Estoque</h1>
          <p>Entrada, saída e inventário de produtos</p>
        </div>
        <div className="actions-row">
          <button className="btn secondary" type="button" onClick={() => abrirMovimento('ENTRADA')}>
            Entrada
          </button>
          <button className="btn secondary" type="button" onClick={() => abrirMovimento('SAIDA')}>
            Saída
          </button>
          <button className="btn secondary" type="button" onClick={() => abrirMovimento('INVENTARIO')}>
            Inventário
          </button>
          <button className="btn" type="button" onClick={abrirNovoProduto}>
            Novo produto
          </button>
        </div>
      </div>

      {ok && <div className="panel" style={{ marginBottom: '1rem', color: 'var(--ok)' }}>{ok}</div>}
      {error && !produtoOpen && !movOpen && (
        <div className="error" style={{ marginBottom: '1rem', display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <span>{error}</span>
          <button className="btn secondary" type="button" onClick={() => carregar().catch(() => {})}>
            Tentar novamente
          </button>
        </div>
      )}

      <div className="panel" style={{ marginBottom: '1rem' }}>
        {produtos.length === 0 ? (
          <div className="empty">Nenhum produto cadastrado.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Produto</th>
                <th>Qtd</th>
                <th>Mínimo</th>
                <th>Unidade</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {produtos.map((p) => (
                <tr key={p.id}>
                  <td>
                    {p.nome}
                    {p.abaixoMinimo && (
                      <span className="badge danger" style={{ marginLeft: '0.4rem' }}>baixo</span>
                    )}
                  </td>
                  <td>{qty(p.quantidade)}</td>
                  <td>{qty(p.estoqueMinimo)}</td>
                  <td>{p.unidade}</td>
                  <td>
                    <div className="actions-row">
                      <button className="btn secondary small" type="button" onClick={() => abrirMovimento('ENTRADA', p.id)}>
                        +
                      </button>
                      <button className="btn secondary small" type="button" onClick={() => abrirMovimento('SAIDA', p.id)}>
                        −
                      </button>
                      <button className="btn secondary small" type="button" onClick={() => abrirMovimento('INVENTARIO', p.id)}>
                        Inv.
                      </button>
                      <button className="btn secondary small" type="button" onClick={() => abrirEdicao(p)}>
                        Editar
                      </button>
                      <button className="btn danger small" type="button" onClick={() => desativar(p.id)}>
                        Desativar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="panel">
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Últimos movimentos</h2>
        {movimentos.length === 0 ? (
          <div className="empty">Sem movimentos ainda.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Data</th>
                <th>Produto</th>
                <th>Tipo</th>
                <th>Qtd</th>
                <th>Antes → Depois</th>
                <th>Obs.</th>
              </tr>
            </thead>
            <tbody>
              {movimentos.map((m) => (
                <tr key={m.id}>
                  <td>{formatDateTime(m.criadoEm)}</td>
                  <td>{m.produtoNome}</td>
                  <td>
                    <span className={`badge ${m.tipo === 'SAIDA' ? 'warn' : 'ok'}`}>{m.tipo}</span>
                  </td>
                  <td>{qty(m.quantidade)}</td>
                  <td>{qty(m.quantidadeAntes)} → {qty(m.quantidadeDepois)}</td>
                  <td>{m.observacao || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <Modal open={produtoOpen} title={editing ? 'Editar produto' : 'Novo produto'} onClose={() => setProdutoOpen(false)}>
        <form onSubmit={salvarProduto}>
          <div className="form-grid">
            <label className="full">
              Nome
              <input
                value={formProduto.nome}
                onChange={(e) => setFormProduto({ ...formProduto, nome: e.target.value })}
                placeholder="Ex.: Gel"
                required
              />
            </label>
            <label>
              Unidade
              <input
                value={formProduto.unidade}
                onChange={(e) => setFormProduto({ ...formProduto, unidade: e.target.value })}
              />
            </label>
            <label>
              Estoque mínimo
              <input
                type="number"
                min={0}
                step="0.001"
                inputMode="decimal"
                value={formProduto.estoqueMinimo}
                onChange={(e) => setFormProduto({ ...formProduto, estoqueMinimo: e.target.value })}
                placeholder="Ex.: 5"
                required
              />
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn secondary" type="button" onClick={() => setProdutoOpen(false)}>Cancelar</button>
            <button className="btn" type="submit">Salvar</button>
          </div>
        </form>
      </Modal>

      <Modal
        open={movOpen}
        title={
          formMov.tipo === 'ENTRADA'
            ? 'Entrada de estoque'
            : formMov.tipo === 'SAIDA'
              ? 'Saída de estoque'
              : 'Inventário'
        }
        onClose={() => setMovOpen(false)}
      >
        <form onSubmit={salvarMovimento}>
          <div className="form-grid">
            <label className="full">
              Produto
              <select
                value={formMov.produtoId}
                onChange={(e) => setFormMov({ ...formMov, produtoId: e.target.value })}
                required
              >
                <option value="">Selecione</option>
                {produtos.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nome} ({qty(p.quantidade)} {p.unidade})
                  </option>
                ))}
              </select>
            </label>
            <label>
              Tipo
              <select
                value={formMov.tipo}
                onChange={(e) => setFormMov({ ...formMov, tipo: e.target.value })}
              >
                <option value="ENTRADA">Entrada</option>
                <option value="SAIDA">Saída</option>
                <option value="INVENTARIO">Inventário</option>
              </select>
            </label>
            <label>
              {labelMov}
              <input
                type="number"
                min={0}
                step="0.001"
                value={formMov.quantidade}
                onChange={(e) => setFormMov({ ...formMov, quantidade: e.target.value })}
                required
              />
            </label>
            <label className="full">
              Observação
              <input
                value={formMov.observacao}
                onChange={(e) => setFormMov({ ...formMov, observacao: e.target.value })}
              />
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn secondary" type="button" onClick={() => setMovOpen(false)}>Cancelar</button>
            <button className="btn" type="submit">Confirmar</button>
          </div>
        </form>
      </Modal>
    </>
  )
}
