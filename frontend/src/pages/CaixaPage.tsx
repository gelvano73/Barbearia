/**
 * Operações de caixa: abertura, fechamento, sangria e suprimento.
 */
import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { caixaApi } from '../services/resources'

/** === Helpers === */
function money(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatDateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function CaixaPage() {
  /** === Estado === */
  const [caixa, setCaixa] = useState(null)
  const [error, setError] = useState('')
  const [abrirOpen, setAbrirOpen] = useState(false)
  const [fecharOpen, setFecharOpen] = useState(false)
  const [movOpen, setMovOpen] = useState(null)
  const [valorAbertura, setValorAbertura] = useState('0')
  const [valorFechamento, setValorFechamento] = useState('')
  const [movValor, setMovValor] = useState('')
  const [movDesc, setMovDesc] = useState('')
  const [obs, setObs] = useState('')

  /** === Carga e operações === */
  const carregar = async () => {
    const { data } = await caixaApi.atual()
    setCaixa(data)
  }

  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar o caixa'))
  }, [])

  const abrir = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await caixaApi.abrir({
        valorAbertura: Number(valorAbertura || 0),
        observacoes: obs || undefined,
      })
      setAbrirOpen(false)
      setObs('')
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao abrir caixa')
    }
  }

  const fechar = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await caixaApi.fechar({
        valorInformado: Number(valorFechamento),
        observacoes: obs || undefined,
      })
      setFecharOpen(false)
      setObs('')
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao fechar caixa')
    }
  }

  const movimento = async (e) => {
    e.preventDefault()
    setError('')
    try {
      // Monta o payload tipado para a API
      const payload = { valor: Number(movValor), descricao: movDesc || undefined }
      if (movOpen === 'sangria') await caixaApi.sangria(payload)
      else await caixaApi.suprimento(payload)
      setMovOpen(null)
      setMovValor('')
      setMovDesc('')
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro no movimento')
    }
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Caixa diário</h1>
          <p>{caixa ? `Aberto em ${formatDateTime(caixa.abertoEm)}` : 'Nenhum caixa aberto'}</p>
        </div>
        <div className="actions-row">
          {!caixa && (
            <button className="btn" type="button" onClick={() => { setError(''); setAbrirOpen(true) }}>
              Abrir caixa
            </button>
          )}
          {caixa && (
            <>
              <button className="btn secondary" type="button" onClick={() => { setError(''); setMovOpen('suprimento') }}>
                Suprimento
              </button>
              <button className="btn secondary" type="button" onClick={() => { setError(''); setMovOpen('sangria') }}>
                Sangria
              </button>
              <button
                className="btn"
                type="button"
                onClick={() => {
                  setValorFechamento(String(caixa.saldoCalculado ?? ''))
                  setError('')
                  setFecharOpen(true)
                }}
              >
                Fechar caixa
              </button>
            </>
          )}
        </div>
      </div>

      {error && <div className="error" style={{ marginBottom: '1rem' }}>{error}</div>}

      {/* === Resumo e movimentos === */}
      {!caixa ? (
        <div className="panel">
          <div className="empty">Abra o caixa para registrar pagamentos e movimentos do dia.</div>
        </div>
      ) : (
        <>
          <div
            className="panel"
            style={{
              display: 'grid',
              gap: '1rem',
              gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
            }}
          >
            <div>
              <div className="subtitle">Abertura</div>
              <strong style={{ fontSize: '1.4rem' }}>{money(caixa.valorAbertura)}</strong>
            </div>
            <div>
              <div className="subtitle">Entradas</div>
              <strong style={{ fontSize: '1.4rem' }}>{money(caixa.totalEntradas)}</strong>
            </div>
            <div>
              <div className="subtitle">Sangrias</div>
              <strong style={{ fontSize: '1.4rem' }}>{money(caixa.totalSangrias)}</strong>
            </div>
            <div>
              <div className="subtitle">Suprimentos</div>
              <strong style={{ fontSize: '1.4rem' }}>{money(caixa.totalSuprimentos)}</strong>
            </div>
            <div>
              <div className="subtitle">Saldo</div>
              <strong style={{ fontSize: '1.4rem' }}>{money(caixa.saldoCalculado)}</strong>
            </div>
          </div>

          <div className="panel" style={{ marginTop: '1rem' }}>
            <h2 style={{ marginTop: 0 }}>Movimentos</h2>
            {!caixa.movimentos?.length ? (
              <div className="empty">Sem movimentos ainda.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Hora</th>
                    <th>Tipo</th>
                    <th>Forma</th>
                    <th>Valor</th>
                    <th>Descrição</th>
                  </tr>
                </thead>
                <tbody>
                  {caixa.movimentos.map((m) => (
                    <tr key={m.id}>
                      <td>{formatDateTime(m.criadoEm)}</td>
                      <td>{m.tipo}</td>
                      <td>{m.formaPagamento || '—'}</td>
                      <td>{money(m.valor)}</td>
                      <td>{m.descricao || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      {/* === Modais === */}
      <Modal open={abrirOpen} title="Abrir caixa" onClose={() => setAbrirOpen(false)}>
        <form onSubmit={abrir}>
          <label>
            Valor de abertura
            <input
              type="number"
              min="0"
              step="0.01"
              value={valorAbertura}
              onChange={(e) => setValorAbertura(e.target.value)}
              required
            />
          </label>
          <label>
            Observações
            <textarea value={obs} onChange={(e) => setObs(e.target.value)} rows={2} />
          </label>
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" style={{ marginTop: '1rem' }}>
            Abrir
          </button>
        </form>
      </Modal>

      <Modal open={fecharOpen} title="Fechar caixa" onClose={() => setFecharOpen(false)}>
        <form onSubmit={fechar}>
          <p>Saldo calculado: {money(caixa?.saldoCalculado)}</p>
          <label>
            Valor informado no fechamento
            <input
              type="number"
              min="0"
              step="0.01"
              value={valorFechamento}
              onChange={(e) => setValorFechamento(e.target.value)}
              required
            />
          </label>
          <label>
            Observações
            <textarea value={obs} onChange={(e) => setObs(e.target.value)} rows={2} />
          </label>
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" style={{ marginTop: '1rem' }}>
            Fechar
          </button>
        </form>
      </Modal>

      <Modal
        open={Boolean(movOpen)}
        title={movOpen === 'sangria' ? 'Sangria' : 'Suprimento'}
        onClose={() => setMovOpen(null)}
      >
        <form onSubmit={movimento}>
          <label>
            Valor
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={movValor}
              onChange={(e) => setMovValor(e.target.value)}
              required
            />
          </label>
          <label>
            Descrição
            <input value={movDesc} onChange={(e) => setMovDesc(e.target.value)} />
          </label>
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" style={{ marginTop: '1rem' }}>
            Confirmar
          </button>
        </form>
      </Modal>
    </>
  )
}
