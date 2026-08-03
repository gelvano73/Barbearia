/**
 * Layout do portal do cliente.
 * Sidebar com links de agendamento, histórico, fidelidade e perfil.
 */
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { temRecurso } from '../data/planos'
import AppShell from './AppShell'

/** === Helpers === */
const linkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'active' : undefined)

export default function PortalLayout() {
  const { auth, logout } = useAuth()
  const podeFidelidade = temRecurso(auth?.plano as string | undefined, 'FIDELIDADE')

  return (
    <AppShell
      brandTitle="BARBA"
      brandSubtitle="PORTAL"
      nav={
        <>
          {/* === Navegação principal === */}
          <NavLink to="/portal" end className={linkClass}>Início</NavLink>
          <NavLink to="/portal/agendar" className={linkClass}>Agendar</NavLink>
          <NavLink to="/portal/assistente" className={linkClass}>IA Atendimento</NavLink>
          <NavLink to="/portal/agendamentos" className={linkClass}>Meus horários</NavLink>
          <NavLink to="/portal/historico" className={linkClass}>Histórico</NavLink>
          {podeFidelidade && <NavLink to="/portal/fidelidade" className={linkClass}>Fidelidade</NavLink>}
          <NavLink to="/portal/perfil" className={linkClass}>Perfil</NavLink>
        </>
      }
      footer={
        <>
          {/* === Rodapé da sidebar === */}
          <div>
            <strong>{auth?.nome}</strong>
            <div>{auth?.nomeBarbearia}</div>
          </div>
          <button className="btn secondary" type="button" onClick={logout}>Sair</button>
        </>
      }
    >
      <Outlet />
    </AppShell>
  )
}
