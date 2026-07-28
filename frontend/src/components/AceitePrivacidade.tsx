/**
 * Checkbox obrigatório de aceite da Política de Privacidade (LGPD).
 */
import { Link } from 'react-router-dom'

type AceitePrivacidadeProps = {
  checked: boolean
  onChange: (checked: boolean) => void
  id?: string
}

export default function AceitePrivacidade({ checked, onChange, id = 'aceite-privacidade' }: AceitePrivacidadeProps) {
  return (
    <label className="aceite-privacidade" htmlFor={id}>
      <input
        id={id}
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        required
      />
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
