// @ts-nocheck
/**
 * Configuração do programa de fidelidade, saldos e resgates.
 */
import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { fidelidadeApi } from '../services/resources'

/** === Helpers === */
function formatDateTime(value) {
  return new Date(value).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function FidelidadePage() {
  /** === Estado === */
  const [config, setConfig] = useState(null)
  const [saldos, setSaldos] = useState([])
  const [configOpen, setConfigOpen] = useState(false)
  const [histOpen, setHistOpen] = useState(false)
  const [selected, setSelected] = useState(null)
  const [historico, setHistorico] = useState([])
  const [form, setForm] = useState({
    pontosPorAtendimento: 1,
    pontosParaResgate: 10,
    descricao: 'A cada 10 cortes = 1 grátis',
    ativo: true,
  })
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')

  /** === Carga de dados === */
  const carregar = async () => {
    const [{ data: cfg }, { data: s }] = await Promise.all([
      fidelidadeApi.config(),
      fidelidadeApi.saldos(),
    ])
    setConfig(cfg)
    setSaldos(s)
    setForm({
      pontosPorAtendimento: cfg.pontosPorAtendimento,
      pontosParaResgate: cfg.pontosParaResgate,
      descricao: cfg.descricao,
      ativo: cfg.ativo,
    })
  }

  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar fidelidade'))
  }, [])

  /** === Ações === */
  const salvarConfig = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await fidelidadeApi.atualizarConfig({
        ...form,
        pontosPorAtendimento: Number(form.pontosPorAtendimento),
        pontosParaResgate: Number(form.pontosParaResgate),
      })
      setConfigOpen(false)
      setOk('Regra atualizada')
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao salvar regra')
    }
  }

  const abrirHistorico = async (item) => {
    setSelected(item)
    setError('')
    try {
      const { data } = await fidelidadeApi.historico(item.clienteId)
      setHistorico(data)
      setHistOpen(true)
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao carregar histórico')
    }
  }

  const resgatar = async (item) => {
    if (!confirm(`Resgatar 1 corte grátis para ${item.clienteNome}? (-${item.pontosParaResgate} pontos)`)) return
    setError('')
    setOk('')
    try {
      await fidelidadeApi.resgatar({ clienteId: item.clienteId })
      setOk(`Resgate feito para ${item.clienteNome}`)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao resgatar')
    }
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Fidelidade</h1>
          <p>{config?.descricao || 'Programa de pontos e resgates'}</p>
        </div>
        <button className="btn secondary" type="button" onClick={() => { setError(''); setConfigOpen(true) }}>
          Configurar regra
        </button>
      </div>

      {/* === Resumo da regra === */}
      {config && (
        <div className="panel confirm-box" style={{ marginBottom: '1rem' }}>
          <strong>{config.ativo ? 'Programa ativo' : 'Programa inativo'}</strong>
          <p>
            {config.pontosPorAtendimento} ponto(s) por atendimento · {config.pontosParaResgate} pontos = 1 grátis
          </p>
        </div>
      )}

      {ok && <div className="panel" style={{ marginBottom: '1rem', color: 'var(--ok)' }}>{ok}</div>}
      {error && !configOpen && !histOpen && (
        <div className="error" style={{ marginBottom: '1rem' }}>{error}</div>
      )}

      {/* === Saldos === */}
      <div className="panel">
        {saldos.length === 0 ? (
          <div className="empty">
            Nenhum ponto ainda. Ao concluir atendimentos, os pontos são creditados automaticamente.
          </div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Cliente</th>
                <th>Pontos</th>
                <th>Acumulados</th>
                <th>Resgates</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {saldos.map((item) => (
                <tr key={item.clienteId}>
                  <td>{item.clienteNome}</td>
                  <td>
                    {item.pontos}
                    {item.podeResgatar && (
                      <span className="badge ok" style={{ marginLeft: '0.4rem' }}>pode resgatar</span>
                    )}
                  </td>
                  <td>{item.pontosAcumulados}</td>
                  <td>{item.resgates}</td>
                  <td>
                    <div className="actions-row">
                      <button className="btn secondary small" type="button" onClick={() => abrirHistorico(item)}>
                        Histórico
                      </button>
                      {item.podeResgatar && (
                        <button className="btn small" type="button" onClick={() => resgatar(item)}>
                          Resgatar
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* === Modais === */}
      <Modal open={configOpen} title="Regra de fidelidade" onClose={() => setConfigOpen(false)}>
        <form onSubmit={salvarConfig}>
          <div className="form-grid">
            <label className="full">
              Descrição
              <input
                value={form.descricao}
                onChange={(e) => setForm({ ...form, descricao: e.target.value })}
                placeholder="A cada 10 cortes = 1 grátis"
                required
              />
            </label>
            <label>
              Pontos por atendimento
              <input
                type="number"
                min={1}
                value={form.pontosPorAtendimento}
                onChange={(e) => setForm({ ...form, pontosPorAtendimento: e.target.value })}
                required
              />
            </label>
            <label>
              Pontos para resgate
              <input
                type="number"
                min={1}
                value={form.pontosParaResgate}
                onChange={(e) => setForm({ ...form, pontosParaResgate: e.target.value })}
                required
              />
            </label>
            <label className="full">
              Ativo
              <select
                value={form.ativo ? 'true' : 'false'}
                onChange={(e) => setForm({ ...form, ativo: e.target.value === 'true' })}
              >
                <option value="true">Sim</option>
                <option value="false">Não</option>
              </select>
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn secondary" type="button" onClick={() => setConfigOpen(false)}>Cancelar</button>
            <button className="btn" type="submit">Salvar</button>
          </div>
        </form>
      </Modal>

      <Modal open={histOpen} title={`Histórico — ${selected?.clienteNome || ''}`} onClose={() => setHistOpen(false)}>
        {historico.length === 0 ? (
          <div className="empty">Sem movimentos.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Data</th>
                <th>Tipo</th>
                <th>Pontos</th>
                <th>Saldo</th>
                <th>Descrição</th>
              </tr>
            </thead>
            <tbody>
              {historico.map((m) => (
                <tr key={m.id}>
                  <td>{formatDateTime(m.criadoEm)}</td>
                  <td>
                    <span className={`badge ${m.tipo === 'RESGATE' ? 'warn' : 'ok'}`}>{m.tipo}</span>
                  </td>
                  <td>{m.tipo === 'RESGATE' ? `-${m.pontos}` : `+${m.pontos}`}</td>
                  <td>{m.saldoApos}</td>
                  <td>{m.descricao || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="modal-actions">
          <button className="btn secondary" type="button" onClick={() => setHistOpen(false)}>Fechar</button>
        </div>
      </Modal>
    </>
  )
}
