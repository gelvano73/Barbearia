// @ts-nocheck
/**
 * Visão da rede de franquias: empresas, vínculo e criação.
 */
import { useEffect, useState } from 'react'
import { franquiasApi } from '../services/resources'

export default function FranquiasPage() {
  const [visao, setVisao] = useState(null)
  const [empresas, setEmpresas] = useState([])
  const [nome, setNome] = useState('')
  const [error, setError] = useState('')

  // Carrega a listagem principal da página
  const carregar = async () => {
    const [v, e] = await Promise.all([franquiasApi.visao(), franquiasApi.empresas()])
    setVisao(v.data)
    setEmpresas(e.data)
  }

  // Effect: carga inicial dos dados
  useEffect(() => {
    carregar().catch(() => setError('Erro ao carregar franquias'))
  }, [])

  // Cria o registro no formulário
  const criar = async (e) => {
    e.preventDefault()
    await franquiasApi.criarEmpresa({ nome })
    setNome('')
    await carregar()
  }

  // Vincula empresa à rede de franquias
  const vincular = async (id) => {
    await franquiasApi.vincular(id)
    await carregar()
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Franquias</h1>
          <p>Multiempresa e multiunidade</p>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      {visao && (
        <div className="panel" style={{ marginBottom: '1rem' }}>
          <p style={{ margin: 0 }}>
            Barbearia atual: <strong>{visao.barbeariaAtualNome}</strong>
            {visao.empresa && <> · Empresa: <strong>{visao.empresa.nome}</strong></>}
          </p>
          <p style={{ color: 'var(--muted)', margin: '0.5rem 0 0' }}>
            Multiempresa: {visao.multiempresa ? 'sim' : 'não'} · Multiunidade: {visao.multiunidade ? 'sim' : 'não'}
          </p>
        </div>
      )}
      <div className="panel" style={{ marginBottom: '1rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Nova empresa (franqueadora)</h2>
        <form onSubmit={criar} className="actions-row">
          <input placeholder="Nome da rede" value={nome} onChange={(e) => setNome(e.target.value)} required />
          <button className="btn" type="submit">Criar</button>
        </form>
        <ul>
          {empresas.map((e) => (
            <li key={e.id} style={{ marginTop: '0.5rem' }}>
              {e.nome} ({e.quantidadeBarbearias} barbearias){' '}
              <button className="btn secondary" type="button" onClick={() => vincular(e.id)}>
                Vincular esta barbearia
              </button>
            </li>
          ))}
        </ul>
      </div>
      <div className="panel">
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Rede / unidades</h2>
        {!visao?.rede?.length ? (
          <div className="empty">Cadastre unidades em /unidades</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Barbearia</th>
                <th>Unidade</th>
                <th>Padrão</th>
              </tr>
            </thead>
            <tbody>
              {visao.rede.map((r, i) => (
                <tr key={i}>
                  <td>{r.barbeariaNome}</td>
                  <td>{r.unidadeNome}</td>
                  <td>{r.padrao ? 'Sim' : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
