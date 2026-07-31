/**
 * Plano e status da assinatura SaaS do tenant + upgrade e backup.
 */
import { useEffect, useState } from 'react'
import { EmptyState, LoadingState } from '../components/LoadingEmpty'
import { assinaturaApi, backupApi } from '../services/resources'

const PLANOS = [
  { id: 'BASIC', nome: 'Basic', valor: 'R$ 79,90/mês' },
  { id: 'PRO', nome: 'Pro', valor: 'R$ 149,90/mês' },
  { id: 'ENTERPRISE', nome: 'Enterprise', valor: 'R$ 299,90/mês' },
]

export default function AssinaturaPage() {
  const [data, setData] = useState(null)
  const [backups, setBackups] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

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
      setError(err.response?.data?.mensagem || 'Falha ao iniciar upgrade')
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
      <div className="page-header">
        <div>
          <h1>Assinatura</h1>
          <p>Plano SaaS, upgrade e backups</p>
        </div>
        <button className="btn secondary" type="button" disabled={busy} onClick={executarBackup}>
          Executar backup
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      {data && (
        <div className="panel">
          <div className="form-grid">
            <div>
              <strong>Plano</strong>
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
        </div>
      )}

      <div className="panel" style={{ marginTop: '1rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Upgrade de plano</h2>
        <p className="empty" style={{ marginBottom: '1rem' }}>
          Sem `MERCADOPAGO_ACCESS_TOKEN`, o checkout abre em modo simulado e ativa o plano ao confirmar.
        </p>
        <div className="form-grid">
          {PLANOS.map((p) => (
            <div key={p.id}>
              <strong>{p.nome}</strong>
              <div>{p.valor}</div>
              <button
                className="btn"
                type="button"
                style={{ marginTop: '0.5rem' }}
                disabled={busy || data?.plano === p.id}
                onClick={() => upgrade(p.id)}
              >
                {data?.plano === p.id ? 'Plano atual' : 'Assinar'}
              </button>
            </div>
          ))}
        </div>
      </div>

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
