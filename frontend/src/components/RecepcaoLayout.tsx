/**
 * Layout da área de recepção/atendimento.
 * Sidebar com clientes, agenda, fila, pagamentos e caixa.
 */
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import AppShell from './AppShell'

const linkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'active' : undefined)

export default function RecepcaoLayout() {
  const { auth, logout } = useAuth()

  return (
    <AppShell
      brandTitle="BARBA"
      brandSubtitle="RECEPÇÃO"
      nav={
        <>
          <NavLink to="/recepcao" end className={linkClass}>Início</NavLink>
          <NavLink to="/recepcao/clientes" className={linkClass}>Clientes</NavLink>
          <NavLink to="/recepcao/agenda" className={linkClass}>Agenda</NavLink>
          <NavLink to="/recepcao/fila" className={linkClass}>Fila</NavLink>
          <NavLink to="/recepcao/pagamentos" className={linkClass}>Pagamentos</NavLink>
          <NavLink to="/recepcao/caixa" className={linkClass}>Caixa</NavLink>
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
