/**
 * Lista e gerencia os agendamentos futuros do cliente (cancelar/reagendar).
 */
import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { portalApi } from '../../services/resources'

/** === Helpers === */
function formatDateTime(value) {
  return new Date(value).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

export default function PortalAgendamentosPage() {
  /** === Estado === */
  const [itens, setItens] = useState([])
  const [reagendarId, setReagendarId] = useState(null)
  const [novaData, setNovaData] = useState('')
  const [error, setError] = useState('')

  /** === Carga e ações === */
  const carregar = async () => {
    const { data } = await portalApi.agendamentos()
    setItens(data)
  }

  useEffect(() => {
    carregar().catch(() => setError('Falha ao carregar agendamentos'))
  }, [])

  const cancelar = async (id) => {
    if (!confirm('Cancelar este agendamento?')) return
    await portalApi.cancelar(id)
    await carregar()
  }

  const confirmarReagendar = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const dataHora = novaData.length === 16 ? `${novaData}:00` : novaData
      await portalApi.reagendar(reagendarId, { dataHora })
      setReagendarId(null)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao reagendar')
    }
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Meus horários</h1>
          <p>Cancele ou reagende quando precisar</p>
        </div>
      </div>
      {/* === Lista === */}
      <div className="panel">
        {itens.length === 0 ? (
          <div className="empty">Nenhum agendamento.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Data</th>
                <th>Barbeiro</th>
                <th>Serviço</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>{formatDateTime(item.dataHora)}</td>
                  <td>{item.barbeiroNome}</td>
                  <td>{item.servico || '—'}</td>
                  <td><span className="badge warn">{item.status}</span></td>
                  <td>
                    <div className="actions-row">
                      {['AGENDADO', 'CONFIRMADO'].includes(item.status) && (
                        <>
                          <button className="btn secondary small" type="button" onClick={() => {
                            setReagendarId(item.id)
                            setNovaData('')
                            setError('')
                          }}>
                            Reagendar
                          </button>
                          <button className="btn danger small" type="button" onClick={() => cancelar(item.id)}>
                            Cancelar
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* === Modal reagendar === */}
      <Modal open={Boolean(reagendarId)} title="Reagendar" onClose={() => setReagendarId(null)}>
        <form onSubmit={confirmarReagendar}>
          <label>
            Nova data e hora
            <input type="datetime-local" value={novaData} onChange={(e) => setNovaData(e.target.value)} required />
          </label>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn secondary" type="button" onClick={() => setReagendarId(null)}>Fechar</button>
            <button className="btn" type="submit">Salvar</button>
          </div>
        </form>
      </Modal>
    </>
  )
}
