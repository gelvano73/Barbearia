// @ts-nocheck
/**
 * Perfil do barbeiro com visualização e upload de foto.
 */
import { useEffect, useState } from 'react'
import FotoField from '../../components/FotoField'
import { barbeiroPortalApi } from '../../services/resources'

export default function BarbeiroPerfilPage() {
  /** === Estado === */
  const [perfil, setPerfil] = useState(null)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [uploading, setUploading] = useState(false)

  /** === Carga e ações === */
  const carregar = async () => {
    const { data } = await barbeiroPortalApi.perfil()
    setPerfil(data)
  }

  useEffect(() => {
    carregar().catch(() => setError('Falha ao carregar perfil'))
  }, [])

  const onFoto = async (file) => {
    setError('')
    setOk('')
    setUploading(true)
    try {
      const { data } = await barbeiroPortalApi.uploadFoto(file)
      setPerfil(data)
      setOk('Foto atualizada')
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro no upload')
    } finally {
      setUploading(false)
    }
  }

  if (!perfil) return <div className="empty">Carregando...</div>

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Perfil</h1>
          <p>Sua foto e dados da equipe</p>
        </div>
      </div>
      {/* === Dados e foto === */}
      <div className="panel">
        <FotoField fotoUrl={perfil.fotoUrl} onUpload={onFoto} disabled={uploading} />
        <div className="form-grid" style={{ marginTop: '1.25rem' }}>
          <label>
            Nome
            <input value={perfil.nome || ''} disabled />
          </label>
          <label>
            Telefone
            <input value={perfil.telefone || '—'} disabled />
          </label>
          <label className="full">
            Especialidade
            <input value={perfil.especialidade || '—'} disabled />
          </label>
        </div>
        {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
        {ok && <div style={{ marginTop: '0.8rem', color: 'var(--ok)' }}>{ok}</div>}
      </div>
    </>
  )
}
