/**
 * Acompanhamento da meta mensal de atendimentos/comissão.
 */
import { useEffect, useState } from 'react'
import { barbeiroPortalApi } from '../../services/resources'

export default function BarbeiroMetaPage() {
  /** === Estado === */
  const [meta, setMeta] = useState(null)

  /** === Carga de dados === */
  useEffect(() => {
    barbeiroPortalApi.meta().then(({ data }) => setMeta(data))
  }, [])

  if (!meta) return <div className="empty">Carregando...</div>

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Meta mensal</h1>
          <p>{meta.mes}/{meta.ano}</p>
        </div>
      </div>
      {/* === Progresso === */}
      <div className="panel">
        <p>Atendimentos: <strong>{meta.atendimentosRealizados}</strong> de <strong>{meta.metaAtendimentos}</strong> ({meta.percentualAtendimentos}%)</p>
        <p>Comissão: <strong>R$ {Number(meta.comissaoRealizada || 0).toFixed(2)}</strong> de <strong>R$ {Number(meta.metaComissao || 0).toFixed(2)}</strong> ({meta.percentualComissao}%)</p>
        {meta.metaAtendimentos === 0 && meta.metaComissao == 0 && (
          <p className="empty">Nenhuma meta definida ainda. Peça ao admin para configurar em Barbeiros → Meta.</p>
        )}
      </div>
    </>
  )
}
