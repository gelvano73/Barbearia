// @ts-nocheck
/**
 * Status e simulação do atendimento via WhatsApp com IA.
 */
import { useEffect, useState } from 'react'
import { whatsappApi } from '../services/resources'

export default function WhatsappPage() {
  const [status, setStatus] = useState(null)
  const [telefone, setTelefone] = useState('11999998888')
  const [mensagem, setMensagem] = useState('oi')
  const [historico, setHistorico] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // Carrega o status da integração
  const carregarStatus = async () => {
    const { data } = await whatsappApi.status()
    setStatus(data)
  }

  // Effect: carga inicial dos dados
  useEffect(() => {
    carregarStatus().catch(() => setError('Não foi possível carregar status do WhatsApp'))
  }, [])

  // Envia mensagem ao assistente
  const enviar = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await whatsappApi.simular({ telefone, mensagem })
      setHistorico((h) => [
        ...h,
        { de: 'cliente', texto: mensagem },
        { de: 'ia', texto: data.resposta, intencao: data.intencao },
      ])
      setMensagem('')
      await carregarStatus()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao simular mensagem')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>WhatsApp · IA</h1>
          <p>Agendamento e respostas automáticas pelo mesmo motor do assistente</p>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      {status && (
        <div className="panel" style={{ marginBottom: '1rem' }}>
          <div className="actions-row" style={{ flexWrap: 'wrap', gap: '1.5rem' }}>
            <div>
              <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>WhatsApp</div>
              <strong>{status.whatsappEnabled ? 'Ativo' : 'Off'}</strong>
            </div>
            <div>
              <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>IA</div>
              <strong>{status.aiEnabled ? status.provider : 'Off'}</strong>
            </div>
            <div>
              <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Envio</div>
              <strong>{status.simularEnvio ? 'Simulado (log)' : 'Cloud API'}</strong>
            </div>
            <div>
              <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Webhook</div>
              <strong style={{ fontSize: '0.9rem' }}>{status.webhookPost}</strong>
            </div>
          </div>
          <p style={{ margin: '0.75rem 0 0', color: 'var(--muted)', fontSize: '0.85rem' }}>
            Configure no Meta: GET/POST <code>{status.webhookGet}</code> · verify token em{' '}
            <code>WHATSAPP_VERIFY_TOKEN</code>. Sem token da Cloud API, as respostas só aparecem aqui e nos logs.
          </p>
        </div>
      )}

      <div className="panel" style={{ marginBottom: '1rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Simular conversa</h2>
        <form onSubmit={enviar} className="form-grid">
          <label>
            Telefone
            <input value={telefone} onChange={(e) => setTelefone(e.target.value)} required />
          </label>
          <label className="full">
            Mensagem
            <input
              value={mensagem}
              onChange={(e) => setMensagem(e.target.value)}
              placeholder='Ex.: quero corte amanhã às 15h'
              required
            />
          </label>
          <button className="btn" type="submit" disabled={loading}>
            {loading ? 'Enviando…' : 'Enviar para a IA'}
          </button>
        </form>
      </div>

      <div className="panel">
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Respostas automáticas</h2>
        {historico.length === 0 ? (
          <div className="empty">Nenhuma mensagem ainda. Experimente: oi · horários · quero corte amanhã às 15h · sim</div>
        ) : (
          <div style={{ display: 'grid', gap: '0.75rem' }}>
            {historico.map((m, i) => (
              <div
                key={i}
                style={{
                  padding: '0.75rem 1rem',
                  borderRadius: 10,
                  background: m.de === 'ia' ? 'rgba(201,162,39,0.12)' : 'rgba(255,255,255,0.04)',
                  whiteSpace: 'pre-wrap',
                }}
              >
                <div style={{ color: 'var(--muted)', fontSize: '0.75rem', marginBottom: 4 }}>
                  {m.de === 'ia' ? `Assistente${m.intencao ? ` · ${m.intencao}` : ''}` : 'Cliente'}
                </div>
                {m.texto}
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  )
}
