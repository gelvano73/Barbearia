/**
 * Configuração fiscal e listagem/emissão de NFS-e (Focus NFe).
 * Exige CNPJ/IM válidos e CPF real do tomador (Receita Federal).
 */
import { useEffect, useState } from 'react'
import CepLookupField from '../components/CepLookupField'
import PasswordInput from '../components/PasswordInput'
import { EmptyState, LoadingState } from '../components/LoadingEmpty'
import { fiscalApi } from '../services/resources'

/** === Helpers === */
function money(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

/** === Estado inicial === */
const emptyConfig = {
  cnpj: '',
  razaoSocial: '',
  inscricaoMunicipal: '',
  codigoMunicipioIbge: '',
  aliquotaIss: '2',
  codigoServicoPadrao: '6.02',
  regimeTributario: 'SIMPLES_NACIONAL',
  optanteSimples: true,
  nfseHabilitada: false,
  nfseToken: '',
  enderecoLogradouro: '',
  enderecoNumero: '',
  enderecoBairro: '',
  enderecoCep: '',
  enderecoUf: '',
  email: '',
  telefone: '',
}

export default function FiscalPage() {
  /** === Estado === */
  const [config, setConfig] = useState(emptyConfig)
  const [meta, setMeta] = useState(null)
  const [notas, setNotas] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')

  /** === Carga e persistência === */
  const carregar = async () => {
    const [{ data: cfg }, { data: lista }] = await Promise.all([
      fiscalApi.config(),
      fiscalApi.listarNotas(),
    ])
    setMeta(cfg)
    setConfig({
      cnpj: cfg.cnpj || '',
      razaoSocial: cfg.razaoSocial || '',
      inscricaoMunicipal: cfg.inscricaoMunicipal || '',
      codigoMunicipioIbge: cfg.codigoMunicipioIbge || '',
      aliquotaIss: cfg.aliquotaIss != null ? String(cfg.aliquotaIss) : '2',
      codigoServicoPadrao: cfg.codigoServicoPadrao || '6.02',
      regimeTributario: cfg.regimeTributario || 'SIMPLES_NACIONAL',
      optanteSimples: Boolean(cfg.optanteSimples),
      nfseHabilitada: Boolean(cfg.nfseHabilitada),
      nfseToken: '',
      enderecoLogradouro: cfg.enderecoLogradouro || '',
      enderecoNumero: cfg.enderecoNumero || '',
      enderecoBairro: cfg.enderecoBairro || '',
      enderecoCep: cfg.enderecoCep || '',
      enderecoUf: cfg.enderecoUf || '',
      email: cfg.email || '',
      telefone: cfg.telefone || '',
    })
    setNotas(lista)
  }

  useEffect(() => {
    setLoading(true)
    carregar()
      .catch(() => setError('Não foi possível carregar dados fiscais'))
      .finally(() => setLoading(false))
  }, [])

  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    setOk('')
    setSaving(true)
    try {
      const payload = {
        ...config,
        aliquotaIss: Number(String(config.aliquotaIss).replace(',', '.')),
        nfseToken: config.nfseToken || undefined,
      }
      const { data } = await fiscalApi.salvarConfig(payload)
      setMeta(data)
      setOk('Configuração fiscal salva.')
      setConfig((c) => ({ ...c, nfseToken: '' }))
    } catch (err) {
      setError(err.response?.data?.mensagem || err.response?.data?.detalhes?.join?.(' · ') || 'Falha ao salvar')
    } finally {
      setSaving(false)
    }
  }

  const consultar = async (id) => {
    try {
      await fiscalApi.consultarNota(id)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Falha ao consultar nota')
    }
  }

  if (loading) return <LoadingState />

  return (
    <div className="page">
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Fiscal / NFS-e</h1>
          <p className="subtitle">
            Nota fiscal de serviços com CPF/CNPJ válidos (Receita Federal) via Focus NFe.
          </p>
        </div>
      </div>

      {/* === Status de prontidão === */}
      {meta && (
        <div className={`banner ${meta.prontoParaEmitir ? 'ok' : 'warn'}`} style={{ marginBottom: '1rem' }}>
          <strong>{meta.prontoParaEmitir ? 'Pronto' : 'Pendente'}:</strong> {meta.mensagemProntidao}
          {' · '}Ambiente: {meta.ambiente}
          {meta.possuiToken ? ' · Token OK' : ' · Sem token'}
        </div>
      )}

      {/* === Configuração do prestador === */}
      <form className="card-form" onSubmit={salvar} style={{ display: 'grid', gap: '0.75rem', maxWidth: 720 }}>
        <h2 style={{ margin: 0, fontSize: '1.1rem' }}>Prestador (sua barbearia)</h2>
        <label>
          CNPJ (Receita Federal)
          <input value={config.cnpj} onChange={(e) => setConfig({ ...config, cnpj: e.target.value })} placeholder="00.000.000/0000-00" />
        </label>
        <label>
          Razão social
          <input value={config.razaoSocial} onChange={(e) => setConfig({ ...config, razaoSocial: e.target.value })} />
        </label>
        <label>
          Inscrição municipal
          <input value={config.inscricaoMunicipal} onChange={(e) => setConfig({ ...config, inscricaoMunicipal: e.target.value })} />
        </label>
        <label>
          Código serviço LC 116 (padrão cabeleireiros: 6.02)
          <input value={config.codigoServicoPadrao} onChange={(e) => setConfig({ ...config, codigoServicoPadrao: e.target.value })} />
        </label>
        <label>
          Alíquota ISS (%)
          <input value={config.aliquotaIss} onChange={(e) => setConfig({ ...config, aliquotaIss: e.target.value })} />
        </label>
        <label>
          Regime tributário
          <select value={config.regimeTributario} onChange={(e) => setConfig({ ...config, regimeTributario: e.target.value })}>
            <option value="SIMPLES_NACIONAL">Simples Nacional</option>
            <option value="MEI">MEI</option>
            <option value="LUCRO_PRESUMIDO">Lucro presumido</option>
            <option value="LUCRO_REAL">Lucro real</option>
          </select>
        </label>
        <label className="check">
          <input type="checkbox" checked={config.optanteSimples} onChange={(e) => setConfig({ ...config, optanteSimples: e.target.checked })} />
          Optante pelo Simples Nacional
        </label>
        <label className="check">
          <input type="checkbox" checked={config.nfseHabilitada} onChange={(e) => setConfig({ ...config, nfseHabilitada: e.target.checked })} />
          Habilitar emissão de NFS-e
        </label>
        <label>
          Token Focus NFe {meta?.possuiToken ? '(deixe em branco para manter)' : ''}
          <PasswordInput value={config.nfseToken} onChange={(e) => setConfig({ ...config, nfseToken: e.target.value })} autoComplete="off" />
        </label>

        <h2 style={{ margin: '0.5rem 0 0', fontSize: '1.1rem' }}>Endereço</h2>
        <CepLookupField
          value={config.enderecoCep}
          onChange={(cep) => {
            setError('')
            setConfig((prev) => ({ ...prev, enderecoCep: cep }))
          }}
          onFound={(end) => {
            setConfig((prev) => ({
              ...prev,
              enderecoCep: end.cep,
              enderecoLogradouro: end.logradouro || prev.enderecoLogradouro,
              enderecoBairro: end.bairro || prev.enderecoBairro,
              enderecoUf: end.uf || prev.enderecoUf,
              codigoMunicipioIbge: end.codigoMunicipioIbge || prev.codigoMunicipioIbge,
            }))
          }}
          onError={setError}
          onInfo={(msg) => {
            setError('')
            setOk(msg)
          }}
        />

        <label>
          Logradouro
          <input value={config.enderecoLogradouro} onChange={(e) => setConfig({ ...config, enderecoLogradouro: e.target.value })} />
        </label>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '0.75rem' }}>
          <label>
            Número
            <input value={config.enderecoNumero} onChange={(e) => setConfig({ ...config, enderecoNumero: e.target.value })} />
          </label>
          <label>
            Bairro
            <input value={config.enderecoBairro} onChange={(e) => setConfig({ ...config, enderecoBairro: e.target.value })} />
          </label>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '0.75rem' }}>
          <label>
            UF
            <input value={config.enderecoUf} onChange={(e) => setConfig({ ...config, enderecoUf: e.target.value.toUpperCase() })} maxLength={2} />
          </label>
          <label>
            Código município IBGE (preenchido pela busca do CEP)
            <input value={config.codigoMunicipioIbge} onChange={(e) => setConfig({ ...config, codigoMunicipioIbge: e.target.value })} maxLength={7} />
          </label>
        </div>
        {error && <div className="error">{error}</div>}
        {ok && <div className="success">{ok}</div>}
        <button className="btn" type="submit" disabled={saving}>
          {saving ? 'Salvando...' : 'Salvar configuração'}
        </button>
      </form>

      {/* === Notas emitidas === */}
      <h2 style={{ marginTop: '2rem' }}>Notas emitidas</h2>
      {notas.length === 0 ? (
        <EmptyState>Nenhuma NFS-e ainda. Após pagamento com CPF do cliente, emita em Pagamentos ou aguarde a emissão automática.</EmptyState>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Nº</th>
                <th>Status</th>
                <th>Tomador</th>
                <th>CPF</th>
                <th>Valor</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {notas.map((n) => (
                <tr key={n.id}>
                  <td>{n.numero || '—'}</td>
                  <td>{n.status}</td>
                  <td>{n.tomadorNome}</td>
                  <td>{n.tomadorCpf}</td>
                  <td>{money(n.valorServicos)}</td>
                  <td>
                    <button type="button" className="btn small secondary" onClick={() => consultar(n.id)}>
                      Atualizar
                    </button>
                    {n.urlPdf && (
                      <a className="btn small" href={n.urlPdf} target="_blank" rel="noreferrer" style={{ marginLeft: 6 }}>
                        PDF
                      </a>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
