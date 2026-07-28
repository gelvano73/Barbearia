/**
 * Layout do painel administrativo (staff/admin).
 * Sidebar com navegação completa e área principal via Outlet.
 */
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import AppShell from './AppShell'

const linkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'active' : undefined)

export default function Layout() {
  const { auth, logout } = useAuth()

  return (
    <AppShell
      brandTitle="BARBA"
      brandSubtitle="SAAS"
      nav={
        <>
          <NavLink to="/" end className={linkClass}>Início</NavLink>
          <NavLink to="/clientes" className={linkClass}>Clientes</NavLink>
          <NavLink to="/barbeiros" className={linkClass}>Barbeiros</NavLink>
          <NavLink to="/servicos" className={linkClass}>Serviços</NavLink>
          <NavLink to="/agendamentos" className={linkClass}>Agendamentos</NavLink>
          <NavLink to="/pagamentos" className={linkClass}>Pagamentos</NavLink>
          <NavLink to="/fidelidade" className={linkClass}>Fidelidade</NavLink>
          <NavLink to="/estoque" className={linkClass}>Estoque</NavLink>
          <NavLink to="/caixa" className={linkClass}>Caixa</NavLink>
          <NavLink to="/comissoes" className={linkClass}>Comissões</NavLink>
          <NavLink to="/relatorios" className={linkClass}>Relatórios</NavLink>
          <NavLink to="/unidades" className={linkClass}>Unidades</NavLink>
          <NavLink to="/whatsapp" className={linkClass}>WhatsApp IA</NavLink>
          <NavLink to="/gestao" className={linkClass}>IA Gestão</NavLink>
          <NavLink to="/checkin" className={linkClass}>Check-in</NavLink>
          <NavLink to="/marketplace" className={linkClass}>Marketplace</NavLink>
          <NavLink to="/franquias" className={linkClass}>Franquias</NavLink>
          <NavLink to="/recepcao" className={linkClass}>Recepção</NavLink>
        </>
      }
      footer={
        <>
          <div>
            <strong>{auth?.nomeBarbearia}</strong>
            <div>{auth?.nome}</div>
          </div>
          <NavLink to="/privacidade" className={linkClass}>Privacidade</NavLink>
          <button className="btn secondary" type="button" onClick={logout}>Sair</button>
        </>
      }
    >
      <Outlet />
    </AppShell>
  )
}
