/**
 * Shell responsivo compartilhado pelos painéis (admin, portal, barbeiro, recepção).
 * No desktop: sidebar fixa. No mobile/tablet: menu hamburger com drawer e overlay.
 */
import { useEffect, useState, type ReactNode } from 'react'
import { useLocation } from 'react-router-dom'

type AppShellProps = {
  brandTitle: string
  brandSubtitle: string
  nav: ReactNode
  footer: ReactNode
  children: ReactNode
}

export default function AppShell({ brandTitle, brandSubtitle, nav, footer, children }: AppShellProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()

  // Fecha o drawer ao navegar (mobile)
  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  // Trava o scroll do body quando o menu está aberto em telas pequenas
  useEffect(() => {
    const mq = window.matchMedia('(max-width: 900px)')
    if (menuOpen && mq.matches) {
      document.body.classList.add('nav-open')
    } else {
      document.body.classList.remove('nav-open')
    }
    return () => document.body.classList.remove('nav-open')
  }, [menuOpen])

  return (
    <div className={`app-shell ${menuOpen ? 'menu-open' : ''}`}>
      <header className="mobile-topbar">
        <button
          type="button"
          className="menu-toggle"
          aria-label={menuOpen ? 'Fechar menu' : 'Abrir menu'}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((v) => !v)}
        >
          <span />
          <span />
          <span />
        </button>
        <div className="mobile-topbar-brand">
          <strong>{brandTitle}</strong>
          <span>{brandSubtitle}</span>
        </div>
      </header>

      <div
        className="sidebar-backdrop"
        role="presentation"
        onClick={() => setMenuOpen(false)}
        aria-hidden={!menuOpen}
      />

      <aside className="sidebar" aria-hidden={false}>
        <div className="sidebar-head">
          <div className="brand">
            {brandTitle}
            <span>{brandSubtitle}</span>
          </div>
          <button
            type="button"
            className="menu-close"
            aria-label="Fechar menu"
            onClick={() => setMenuOpen(false)}
          >
            ✕
          </button>
        </div>
        <nav className="nav" onClick={(e) => {
          // Fecha ao clicar em um link
          if ((e.target as HTMLElement).closest('a')) setMenuOpen(false)
        }}>
          {nav}
        </nav>
        <div className="sidebar-footer">{footer}</div>
      </aside>

      <main className="main">{children}</main>
    </div>
  )
}
