/**
 * Checkbox obrigatório de aceite da Política de Privacidade (LGPD).
 */
import { Link } from 'react-router-dom'

/** === Tipos === */
type AceitePrivacidadeProps = {
  checked: boolean
  onChange: (checked: boolean) => void
  id?: string
}

export default function AceitePrivacidade({ checked, onChange, id = 'aceite-privacidade' }: AceitePrivacidadeProps) {
  return (
    <label className="aceite-privacidade" htmlFor={id}>
      {/* === Checkbox === */}
      <input
        id={id}
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        required
      />
      {/* === Texto e link === */}
      <span>
        Li e aceito a{' '}
        <Link to="/privacidade" target="_blank" rel="noopener noreferrer">
          Política de Privacidade
        </Link>
        , autorizando o tratamento dos meus dados conforme a LGPD.
      </span>
    </label>
  )
}
