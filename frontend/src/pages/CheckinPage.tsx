// @ts-nocheck
/**
 * Check-in de clientes do dia (manual e reconhecimento facial).
 */
import { useEffect, useState } from 'react'
import { checkinApi, clientesApi } from '../services/resources'

export default function CheckinPage() {
  /** === Estado === */
  const [hoje, setHoje] = useState([])
  const [clientes, setClientes] = useState([])
  const [clienteId, setClienteId] = useState('')
  const [file, setFile] = useState(null)
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')

  /** === Carga de dados === */
  const carregar = async () => {
    const [h, c] = await Promise.all([checkinApi.hoje(), clientesApi.listar(true)])
    setHoje(h.data)
    setClientes(c.data)
  }

  useEffect(() => {
    carregar().catch(() => setError('Erro ao carregar check-ins'))
  }, [])

  /** === Ações de check-in === */
  const cadastrarFace = async () => {
    setError('')
    setMsg('')
    if (!clienteId || !file) {
      setError('Selecione cliente e foto')
      return
    }
    try {
      const { data } = await checkinApi.cadastrarFace(clienteId, file)
      setMsg(data.mensagem)
      setFile(null)
    } catch (e) {
      setError(e.response?.data?.mensagem || 'Falha no cadastro facial')
    }
  }

  const checkinFacial = async () => {
    setError('')
    setMsg('')
    if (!file) {
      setError('Envie a foto para reconhecimento')
      return
    }
    try {
      const { data } = await checkinApi.facial(file)
      setMsg(`${data.mensagem}: ${data.clienteNome} (${data.confianca}%)`)
      setFile(null)
      await carregar()
    } catch (e) {
      setError(e.response?.data?.mensagem || 'Não reconhecido')
    }
  }

  const checkinManual = async () => {
    if (!clienteId) return
    try {
      const { data } = await checkinApi.manual(clienteId)
      setMsg(data.mensagem)
      await carregar()
    } catch (e) {
      setError(e.response?.data?.mensagem || 'Erro no check-in')
    }
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Check-in facial</h1>
          <p>Cadastre a face e faça check-in do cliente</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      {msg && <p style={{ color: 'var(--ok)' }}>{msg}</p>}

      {/* === Formulário === */}
      <div className="panel" style={{ marginBottom: '1rem' }}>
        <div className="form-grid">
          <label>
            Cliente
            <select value={clienteId} onChange={(e) => setClienteId(e.target.value)}>
              <option value="">Selecione</option>
              {clientes.map((c) => (
                <option key={c.id} value={c.id}>{c.nome}</option>
              ))}
            </select>
          </label>
          <label className="full">
            Foto
            <input type="file" accept="image/*" onChange={(e) => setFile(e.target.files?.[0] || null)} />
          </label>
        </div>
        <div className="actions-row" style={{ marginTop: '1rem' }}>
          <button className="btn secondary" type="button" onClick={cadastrarFace}>Cadastrar face</button>
          <button className="btn" type="button" onClick={checkinFacial}>Check-in facial</button>
          <button className="btn secondary" type="button" onClick={checkinManual}>Check-in manual</button>
        </div>
        <p style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>
          MVP local: o reconhecimento usa assinatura da imagem (ideal testar com a mesma foto cadastrada).
        </p>
      </div>

      {/* === Check-ins de hoje === */}
      <div className="panel">
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Check-ins de hoje</h2>
        {hoje.length === 0 ? (
          <div className="empty">Nenhum check-in hoje.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Hora</th>
                <th>Cliente</th>
                <th>Método</th>
                <th>Confiança</th>
              </tr>
            </thead>
            <tbody>
              {hoje.map((c) => (
                <tr key={c.id}>
                  <td>{c.criadoEm ? new Date(c.criadoEm).toLocaleTimeString('pt-BR') : '—'}</td>
                  <td>{c.clienteNome}</td>
                  <td>{c.metodo}</td>
                  <td>{c.confianca != null ? `${c.confianca}%` : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
