/**
 * Shell responsivo compartilhado pelos painéis (admin, portal, barbeiro, recepção).
 * No desktop: sidebar fixa. No mobile/tablet: menu hamburger com drawer e overlay.
 */
import { useEffect, useState, type ReactNode } from 'react'
import { useLocation } from 'react-router-dom'

/** === Constantes === */
const MOBILE_MQ = '(max-width: 1024px)'

/** === Tipos === */
type AppShellProps = {
  brandTitle: string
  brandSubtitle: string
  nav: ReactNode
  footer: ReactNode
  children: ReactNode
}

export default function AppShell({ brandTitle, brandSubtitle, nav, footer, children }: AppShellProps) {
  /** === Estado === */
  const [menuOpen, setMenuOpen] = useState(false)
  const [isMobile, setIsMobile] = useState(() =>
    typeof window !== 'undefined' ? window.matchMedia(MOBILE_MQ).matches : false,
  )
  const location = useLocation()

  /** === Efeitos de menu === */
  useEffect(() => {
    const mq = window.matchMedia(MOBILE_MQ)
    const onChange = () => {
      setIsMobile(mq.matches)
      if (!mq.matches) setMenuOpen(false)
    }
    onChange()
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])

  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (!menuOpen) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMenuOpen(false)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [menuOpen])

  useEffect(() => {
    // limpa travas de scroll deixadas por modal/menu
    document.body.classList.remove('modal-open')
    document.body.style.overflow = ''
    document.documentElement.style.overflow = ''
  }, [location.pathname])

  useEffect(() => {
    if (menuOpen && isMobile) {
      document.body.classList.add('nav-open')
    } else {
      document.body.classList.remove('nav-open')
    }
    return () => document.body.classList.remove('nav-open')
  }, [menuOpen, isMobile])

  const drawerHidden = isMobile && !menuOpen

  return (
    <div className={`app-shell ${menuOpen ? 'menu-open' : ''} ${isMobile ? 'is-mobile' : ''}`}>
      {/* === Topbar mobile === */}
      <header className="mobile-topbar">
        <button
          type="button"
          className="menu-toggle"
          aria-label={menuOpen ? 'Fechar menu' : 'Abrir menu'}
          aria-expanded={menuOpen}
          aria-controls="app-sidebar"
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

      {/* === Backdrop === */}
      <div
        className="sidebar-backdrop"
        role="presentation"
        onClick={() => setMenuOpen(false)}
        aria-hidden={!menuOpen}
      />

      {/* === Sidebar === */}
      <aside
        id="app-sidebar"
        className="sidebar"
        aria-hidden={drawerHidden}
      >
        <div className="sidebar-head">
          <div className="brand">
            {brandTitle}
            <span>{brandSubtitle}</span>
          </div>
        </div>
        <nav
          className="nav"
          onClick={(e) => {
            if ((e.target as HTMLElement).closest('a')) setMenuOpen(false)
          }}
        >
          {nav}
        </nav>
        <div className="sidebar-footer">{footer}</div>
      </aside>

      {/* === Conteúdo === */}
      <main
        className="main"
        id="app-main"
        style={
          isMobile
            ? undefined
            : { overflowY: 'scroll', height: '100%', minHeight: 0, WebkitOverflowScrolling: 'touch' }
        }
      >
        {children}
      </main>
    </div>
  )
}
