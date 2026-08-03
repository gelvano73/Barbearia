// @ts-nocheck
/**
 * Histórico de atendimentos do cliente com opção de avaliar.
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

export default function PortalHistoricoPage() {
  /** === Estado === */
  const [itens, setItens] = useState([])
  const [avaliarItem, setAvaliarItem] = useState(null)
  const [nota, setNota] = useState(5)
  const [comentario, setComentario] = useState('')
  const [error, setError] = useState('')

  /** === Carga e ações === */
  const carregar = async () => {
    const { data } = await portalApi.historico()
    setItens(data)
  }

  useEffect(() => {
    carregar().catch(() => setError('Falha ao carregar histórico'))
  }, [])

  const enviarAvaliacao = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await portalApi.avaliar({
        agendamentoId: avaliarItem.id,
        nota: Number(nota),
        comentario: comentario || undefined,
      })
      setAvaliarItem(null)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao avaliar')
    }
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Histórico</h1>
          <p>Serviços concluídos e avaliações</p>
        </div>
      </div>
      {/* === Lista === */}
      <div className="panel">
        {itens.length === 0 ? (
          <div className="empty">Nenhum serviço concluído ainda.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Data</th>
                <th>Barbeiro</th>
                <th>Serviço</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>{formatDateTime(item.dataHora)}</td>
                  <td>{item.barbeiroNome}</td>
                  <td>{item.servico || '—'}</td>
                  <td>
                    {item.podeAvaliar && (
                      <button
                        className="btn secondary small"
                        type="button"
                        onClick={() => {
                          setAvaliarItem(item)
                          setNota(5)
                          setComentario('')
                          setError('')
                        }}
                      >
                        Avaliar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* === Modal avaliar === */}
      <Modal open={Boolean(avaliarItem)} title="Avaliar barbeiro" onClose={() => setAvaliarItem(null)}>
        <form onSubmit={enviarAvaliacao}>
          <label>
            Nota (1 a 5)
            <input type="number" min={1} max={5} value={nota} onChange={(e) => setNota(e.target.value)} required />
          </label>
          <label style={{ marginTop: '0.8rem' }}>
            Comentário
            <textarea rows={3} value={comentario} onChange={(e) => setComentario(e.target.value)} />
          </label>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn secondary" type="button" onClick={() => setAvaliarItem(null)}>Fechar</button>
            <button className="btn" type="submit">Enviar</button>
          </div>
        </form>
      </Modal>
    </>
  )
}
