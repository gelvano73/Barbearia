/**
 * Estados vazios e loading reutilizáveis.
 */
import type { ReactNode } from 'react'

/** === Estado vazio === */
export function EmptyState({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>
}

/** === Estado de carregamento === */
export function LoadingState({ label = 'Carregando…' }: { label?: string }) {
  return (
    <div className="loading-state" role="status" aria-live="polite">
      {/* === Spinner === */}
      <span className="loading-spinner" />
      <span>{label}</span>
    </div>
  )
}
