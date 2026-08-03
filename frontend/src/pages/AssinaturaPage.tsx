/**
 * Plano e status da assinatura SaaS do tenant + upgrade e backup.
 */
import { useEffect, useState } from 'react'
import { EmptyState, LoadingState } from '../components/LoadingEmpty'
import { PLANOS_PAGOS, TAXA_IMPLANTACAO_LABEL, TRIAL_INFO } from '../data/planos'
import { assinaturaApi, backupApi } from '../services/resources'

export default function AssinaturaPage() {
  /** === Estado === */
  const [data, setData] = useState(null)
  const [backups, setBackups] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  /** === Carga e ações === */
  const carregar = async () => {
    const [assinatura, listaBackup] = await Promise.all([
      assinaturaApi.status(),
      backupApi.listar().catch(() => ({ data: [] })),
    ])
    setData(assinatura.data)
    setBackups(listaBackup.data || [])
  }

  useEffect(() => {
    carregar()
      .catch((err) => setError(err.response?.data?.mensagem || 'Não foi possível carregar assinatura'))
      .finally(() => setLoading(false))
  }, [])

  const upgrade = async (plano) => {
    setError('')
    setBusy(true)
    try {
      const { data: checkout } = await assinaturaApi.upgrade(plano)
      if (checkout.checkoutUrl) {
        window.open(checkout.checkoutUrl, '_blank', 'noopener,noreferrer')
      }
      await carregar()
    } catch (err) {
      const data = err.response?.data
      const detalhes = Array.isArray(data?.detalhes) ? data.detalhes.join(' · ') : ''
      setError(
        detalhes ||
          data?.mensagem ||
          err.message ||
          'Falha ao iniciar upgrade. Verifique a API e o Mercado Pago.',
      )
    } finally {
      setBusy(false)
    }
  }

  const executarBackup = async () => {
    setError('')
    setBusy(true)
    try {
      await backupApi.executar()
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Falha no backup')
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <LoadingState label="Carregando assinatura…" />

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Assinatura</h1>
          <p>Planos Barba SaaS — escolha o pacote da sua barbearia</p>
        </div>
        <button className="btn secondary" type="button" disabled={busy} onClick={executarBackup}>
          Executar backup
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      {/* === Status do plano === */}
      {data && (
        <div className="panel">
          <div className="form-grid">
            <div>
              <strong>Plano atual</strong>
              <div>{data.plano || 'TRIAL'}</div>
            </div>
            <div>
              <strong>Status</strong>
              <div>
                <span className={`badge ${data.status === 'ATIVA' ? 'ok' : 'warn'}`}>
                  {data.status || '—'}
                </span>
              </div>
            </div>
            <div>
              <strong>Vence em</strong>
              <div>
                {data.venceEm ? new Date(data.venceEm).toLocaleString('pt-BR') : '—'}
              </div>
            </div>
            <div>
              <strong>Dias restantes</strong>
              <div>{data.diasRestantes ?? '—'}</div>
            </div>
          </div>
          {data.plano === 'TRIAL' && (
            <p className="subtitle" style={{ margin: '0.75rem 0 0' }}>
              {TRIAL_INFO.descricao} Depois, escolha Basic, Pro ou Enterprise.
            </p>
          )}
        </div>
      )}

      {/* === Tabela comercial === */}
      <div className="panel" style={{ marginTop: '1rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Tabela de planos</h2>
        <p className="subtitle" style={{ marginTop: 0 }}>
          Implantação opcional: {TAXA_IMPLANTACAO_LABEL}. Mensalidade cobrada via Mercado Pago.
        </p>
        <div className="planos-grid">
          {PLANOS_PAGOS.map((p) => (
            <article
              key={p.id}
              className={`plano-card${p.destaque ? ' destaque' : ''}${data?.plano === p.id ? ' atual' : ''}`}
            >
              {p.destaque && <div className="plano-selo">Mais escolhido</div>}
              <h3>{p.nome}</h3>
              <p className="plano-preco">{p.precoLabel}</p>
              <p className="subtitle">{p.descricao}</p>

              <strong className="plano-bloco-titulo">Limites</strong>
              <ul className="plano-lista">
                {p.limites.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>

              <strong className="plano-bloco-titulo">Inclui</strong>
              <ul className="plano-lista">
                {p.recursos.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>

              <button
                className="btn"
                type="button"
                style={{ marginTop: 'auto', width: '100%' }}
                disabled={busy || data?.plano === p.id}
                onClick={() => upgrade(p.id)}
              >
                {data?.plano === p.id ? 'Plano atual' : `Assinar ${p.nome}`}
              </button>
            </article>
          ))}
        </div>
      </div>

      {/* === Backups === */}
      <div className="panel" style={{ marginTop: '1rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Backups recentes</h2>
        {backups.length === 0 ? (
          <EmptyState>Nenhum backup local ainda.</EmptyState>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Criado em</th>
                <th>OK</th>
              </tr>
            </thead>
            <tbody>
              {backups.map((b) => (
                <tr key={b.nome}>
                  <td>{b.nome}</td>
                  <td>{b.criadoEm ? new Date(b.criadoEm).toLocaleString('pt-BR') : '—'}</td>
                  <td>{b.ok ? 'sim' : 'não'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
