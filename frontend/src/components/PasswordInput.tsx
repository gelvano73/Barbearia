/**
 * Campo de senha com botão de mostrar/ocultar (ícone de olho).
 */
import { useState, type InputHTMLAttributes } from 'react'

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>

function EyeIcon({ open }: { open: boolean }) {
  if (open) {
    return (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path
          d="M3 3l18 18M10.6 10.6a2.5 2.5 0 003.5 3.5M9.9 5.2A10.4 10.4 0 0112 5c5 0 9.3 3.1 11 7.5a11.7 11.7 0 01-4.1 5.1M6.1 6.1A11.7 11.7 0 001 12.5C2.7 16.9 7 20 12 20c1.4 0 2.7-.2 4-.7"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    )
  }
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M1 12.5C2.7 8.1 7 5 12 5s9.3 3.1 11 7.5C21.3 16.9 17 20 12 20S2.7 16.9 1 12.5z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="12.5" r="3.2" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

export default function PasswordInput({ className, ...props }: Props) {
  const [show, setShow] = useState(false)

  return (
    <div className={`password-field${className ? ` ${className}` : ''}`}>
      <input {...props} type={show ? 'text' : 'password'} />
      <button
        type="button"
        className="password-toggle"
        aria-label={show ? 'Ocultar senha' : 'Mostrar senha'}
        aria-pressed={show}
        tabIndex={-1}
        onClick={() => setShow((v) => !v)}
      >
        <EyeIcon open={show} />
      </button>
    </div>
  )
}
