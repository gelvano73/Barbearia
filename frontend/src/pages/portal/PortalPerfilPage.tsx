// @ts-nocheck
/**
 * Edição do perfil e foto do cliente autenticado.
 */
import { useEffect, useState } from 'react'
import FotoField from '../../components/FotoField'
import { portalApi } from '../../services/resources'

export default function PortalPerfilPage() {
  const [perfil, setPerfil] = useState(null)
  const [nome, setNome] = useState('')
  const [telefone, setTelefone] = useState('')
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [uploading, setUploading] = useState(false)

  // Carrega a listagem principal da página
  const carregar = async () => {
    const { data } = await portalApi.perfil()
    setPerfil(data)
    setNome(data.nome || '')
    setTelefone(data.telefone || '')
  }

  // Effect: carga inicial dos dados
  useEffect(() => {
    carregar().catch(() => setError('Falha ao carregar perfil'))
  }, [])

  // Salva criação ou edição do formulário
  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    setOk('')
    try {
      const { data } = await portalApi.atualizarPerfil({ nome, telefone })
      setPerfil(data)
      setOk('Perfil atualizado')
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao salvar')
    }
  }

  // Envia a foto de perfil
  const onFoto = async (file) => {
    setError('')
    setOk('')
    setUploading(true)
    try {
      const { data } = await portalApi.uploadFoto(file)
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
      <div className="page-header">
        <div>
          <h1>Perfil</h1>
          <p>Seus dados e foto</p>
        </div>
      </div>
      <div className="panel">
        <FotoField fotoUrl={perfil.fotoUrl} onUpload={onFoto} disabled={uploading} />
        <form onSubmit={salvar} style={{ marginTop: '1.25rem' }}>
          <div className="form-grid">
            <label>
              Nome
              <input value={nome} onChange={(e) => setNome(e.target.value)} required />
            </label>
            <label>
              Telefone
              <input value={telefone} onChange={(e) => setTelefone(e.target.value)} required />
            </label>
            <label className="full">
              Email
              <input value={perfil.email || ''} disabled />
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          {ok && <div style={{ marginTop: '0.8rem', color: 'var(--ok)' }}>{ok}</div>}
          <div className="modal-actions">
            <button className="btn" type="submit">Salvar</button>
          </div>
        </form>
      </div>
    </>
  )
}
