/**
 * Configuração da grade de horários de trabalho do barbeiro.
 */
import { useEffect, useState } from 'react'
import { barbeiroPortalApi } from '../../services/resources'

/** === Constantes === */
const DIAS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']

/** === Estado inicial === */
const defaultRows = () => DIAS.map((_, i) => ({
  diaSemana: i,
  horaInicio: '09:00',
  horaFim: '18:00',
  ativo: i >= 1 && i <= 5,
}))

export default function BarbeiroHorariosPage() {
  /** === Estado === */
  const [rows, setRows] = useState(defaultRows())
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  /** === Carga e ações === */
  useEffect(() => {
    barbeiroPortalApi.horarios()
      .then(({ data }) => {
        if (!data.length) return
        const map = Object.fromEntries(data.map((h) => [h.diaSemana, h]))
        setRows(DIAS.map((_, i) => {
          const h = map[i]
          return {
            diaSemana: i,
            horaInicio: h ? String(h.horaInicio).slice(0, 5) : '09:00',
            horaFim: h ? String(h.horaFim).slice(0, 5) : '18:00',
            ativo: h ? Boolean(h.ativo) : false,
          }
        }))
      })
      .catch(() => setError('Falha ao carregar horários'))
  }, [])

  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    setMessage('')
    try {
      await barbeiroPortalApi.salvarHorarios({
        horarios: rows.map((r) => ({
          ...r,
          horaInicio: `${r.horaInicio}:00`.slice(0, 8),
          horaFim: `${r.horaFim}:00`.slice(0, 8),
        })),
      })
      setMessage('Horários salvos')
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao salvar')
    }
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Horários</h1>
          <p>Gestão da sua disponibilidade semanal</p>
        </div>
      </div>
      {/* === Grade semanal === */}
      <div className="panel">
        <form onSubmit={salvar}>
          <table className="table">
            <thead>
              <tr>
                <th>Dia</th>
                <th>Ativo</th>
                <th>Início</th>
                <th>Fim</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row, idx) => (
                <tr key={row.diaSemana}>
                  <td>{DIAS[row.diaSemana]}</td>
                  <td>
                    <input
                      type="checkbox"
                      checked={row.ativo}
                      onChange={(e) => {
                        const next = [...rows]
                        next[idx] = { ...row, ativo: e.target.checked }
                        setRows(next)
                      }}
                    />
                  </td>
                  <td>
                    <input
                      type="time"
                      value={row.horaInicio}
                      onChange={(e) => {
                        const next = [...rows]
                        next[idx] = { ...row, horaInicio: e.target.value }
                        setRows(next)
                      }}
                    />
                  </td>
                  <td>
                    <input
                      type="time"
                      value={row.horaFim}
                      onChange={(e) => {
                        const next = [...rows]
                        next[idx] = { ...row, horaFim: e.target.value }
                        setRows(next)
                      }}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {error && <div className="error">{error}</div>}
          {message && <div style={{ color: 'var(--ok)' }}>{message}</div>}
          <div className="modal-actions">
            <button className="btn" type="submit">Salvar</button>
          </div>
        </form>
      </div>
    </>
  )
}
