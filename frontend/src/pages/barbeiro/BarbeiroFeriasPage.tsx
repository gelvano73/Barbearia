/**
 * Solicitação e acompanhamento de férias do barbeiro.
 */
import { useEffect, useState } from 'react'
import { barbeiroPortalApi } from '../../services/resources'

export default function BarbeiroFeriasPage() {
  const [itens, setItens] = useState([])
  const [form, setForm] = useState({ dataInicio: '', dataFim: '', motivo: '' })
  const [error, setError] = useState('')

  // Carrega a listagem principal da página
  const carregar = async () => {
    const { data } = await barbeiroPortalApi.ferias()
    setItens(data)
  }

  // Effect: carga inicial dos dados
  useEffect(() => {
    carregar().catch(() => setError('Falha ao carregar férias'))
  }, [])

  // Solicita a ação (recuperação/férias)
  const solicitar = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await barbeiroPortalApi.solicitarFerias(form)
      setForm({ dataInicio: '', dataFim: '', motivo: '' })
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao solicitar')
    }
  }

  // Cancela o item selecionado
  const cancelar = async (id) => {
    await barbeiroPortalApi.cancelarFerias(id)
    await carregar()
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Férias</h1>
          <p>Controle de folgas e férias</p>
        </div>
      </div>
      <div className="panel" style={{ marginBottom: '1rem' }}>
        <form onSubmit={solicitar}>
          <div className="form-grid">
            <label>
              Início
              <input type="date" value={form.dataInicio} onChange={(e) => setForm({ ...form, dataInicio: e.target.value })} required />
            </label>
            <label>
              Fim
              <input type="date" value={form.dataFim} onChange={(e) => setForm({ ...form, dataFim: e.target.value })} required />
            </label>
            <label className="full">
              Motivo
              <input value={form.motivo} onChange={(e) => setForm({ ...form, motivo: e.target.value })} />
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn" type="submit">Solicitar</button>
          </div>
        </form>
      </div>
      <div className="panel">
        {itens.length === 0 ? (
          <div className="empty">Nenhuma solicitação.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Período</th>
                <th>Motivo</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>{item.dataInicio} → {item.dataFim}</td>
                  <td>{item.motivo || '—'}</td>
                  <td><span className="badge warn">{item.status}</span></td>
                  <td>
                    {item.status === 'SOLICITADO' && (
                      <button className="btn danger small" type="button" onClick={() => cancelar(item.id)}>Cancelar</button>
                    )}
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
