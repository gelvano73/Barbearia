// @ts-nocheck
/**
 * Campo de upload/exibição de foto de perfil.
 * Aceita JPEG/PNG/WEBP e delega o envio ao callback onUpload.
 */
export default function FotoField({
  fotoUrl,
  onUpload,
  disabled = false,
  hint = 'JPEG, PNG ou WEBP · máx. 2MB',
}) {
  /** === Upload === */
  const onChange = async (e) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file || disabled) return
    await onUpload(file)
  }

  return (
    <div className="foto-field">
      {/* === Avatar === */}
      <div className="foto-avatar">
        {fotoUrl ? (
          <img src={fotoUrl} alt="Foto" />
        ) : (
          <span className="foto-placeholder">Sem foto</span>
        )}
      </div>
      {/* === Ações === */}
      <div className="foto-actions">
        <label className={`btn secondary small ${disabled ? 'is-disabled' : ''}`}>
          {fotoUrl ? 'Trocar foto' : 'Enviar foto'}
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            hidden
            disabled={disabled}
            onChange={onChange}
          />
        </label>
        <span className="foto-hint">{disabled ? 'Salve o cadastro para enviar a foto' : hint}</span>
      </div>
    </div>
  )
}
