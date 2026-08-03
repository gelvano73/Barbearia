/**
 * Layout do painel administrativo (staff/admin).
 * Sidebar com navegação completa e área principal via Outlet.
 */
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { temRecurso } from '../data/planos'
import AppShell from './AppShell'

const linkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'active' : undefined)

export default function Layout() {
  const { auth, logout } = useAuth()
  const plano = auth?.plano as string | undefined
  const pode = (recurso: Parameters<typeof temRecurso>[1]) => temRecurso(plano, recurso)

  return (
    <AppShell
      brandTitle="BARBA"
      brandSubtitle="SAAS"
      nav={
        <>
          {/* === Navegação principal === */}
          <NavLink to="/" end className={linkClass}>Início</NavLink>
          <NavLink to="/clientes" className={linkClass}>Clientes</NavLink>
          <NavLink to="/barbeiros" className={linkClass}>Barbeiros</NavLink>
          <NavLink to="/servicos" className={linkClass}>Serviços</NavLink>
          <NavLink to="/agendamentos" className={linkClass}>Agendamentos</NavLink>
          <NavLink to="/pagamentos" className={linkClass}>Pagamentos</NavLink>
          {pode('NFSE') && <NavLink to="/fiscal" className={linkClass}>Fiscal / NFS-e</NavLink>}
          {pode('FIDELIDADE') && <NavLink to="/fidelidade" className={linkClass}>Fidelidade</NavLink>}
          {pode('ESTOQUE') && <NavLink to="/estoque" className={linkClass}>Estoque</NavLink>}
          <NavLink to="/caixa" className={linkClass}>Caixa</NavLink>
          {pode('COMISSOES') && <NavLink to="/comissoes" className={linkClass}>Comissões</NavLink>}
          <NavLink to="/relatorios" className={linkClass}>Relatórios</NavLink>
          <NavLink to="/unidades" className={linkClass}>Unidades</NavLink>
          {pode('WHATSAPP') && <NavLink to="/whatsapp" className={linkClass}>WhatsApp IA</NavLink>}
          {pode('IA_GESTAO') && <NavLink to="/gestao" className={linkClass}>IA Gestão</NavLink>}
          {pode('CHECKIN') && <NavLink to="/checkin" className={linkClass}>Check-in</NavLink>}
          {pode('MARKETPLACE') && <NavLink to="/marketplace" className={linkClass}>Marketplace</NavLink>}
          {pode('FRANQUIAS') && <NavLink to="/franquias" className={linkClass}>Franquias</NavLink>}
          <NavLink to="/assinatura" className={linkClass}>Assinatura</NavLink>
          <NavLink to="/recepcao" className={linkClass}>Recepção</NavLink>
        </>
      }
      footer={
        <>
          {/* === Rodapé da sidebar === */}
          <div>
            <strong>{auth?.nomeBarbearia}</strong>
            <div>{auth?.nome}</div>
            {plano && <div style={{ opacity: 0.75, fontSize: '0.85em' }}>Plano {plano}</div>}
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
