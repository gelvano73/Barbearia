/**
 * Layout do portal do cliente.
 * Sidebar com links de agendamento, histórico, fidelidade e perfil.
 */
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import AppShell from './AppShell'

const linkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'active' : undefined)

export default function PortalLayout() {
  const { auth, logout } = useAuth()

  return (
    <AppShell
      brandTitle="BARBA"
      brandSubtitle="PORTAL"
      nav={
        <>
          <NavLink to="/portal" end className={linkClass}>Início</NavLink>
          <NavLink to="/portal/agendar" className={linkClass}>Agendar</NavLink>
          <NavLink to="/portal/assistente" className={linkClass}>IA Atendimento</NavLink>
          <NavLink to="/portal/agendamentos" className={linkClass}>Meus horários</NavLink>
          <NavLink to="/portal/historico" className={linkClass}>Histórico</NavLink>
          <NavLink to="/portal/fidelidade" className={linkClass}>Fidelidade</NavLink>
          <NavLink to="/portal/perfil" className={linkClass}>Perfil</NavLink>
        </>
      }
      footer={
        <>
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
