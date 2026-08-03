/**
 * Modal reutilizável com backdrop.
 * Renderiza no body (portal) para não ficar preso ao overflow/flex do layout.
 * Fecha ao clicar fora; o conteúdo interno impede o fechamento acidental.
 */
import { useEffect, type ReactNode, type MouseEvent } from 'react'
import { createPortal } from 'react-dom'

/** === Tipos === */
type ModalProps = {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
}

export default function Modal({ open, title, onClose, children }: ModalProps) {
  /** === Efeito de scroll === */
  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [open])

  if (!open) return null

  return createPortal(
    <div className="modal-backdrop" onClick={onClose} role="presentation">
      {/* === Diálogo === */}
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e: MouseEvent) => e.stopPropagation()}
      >
        <h2>{title}</h2>
        {children}
      </div>
    </div>,
    document.body,
  )
}
