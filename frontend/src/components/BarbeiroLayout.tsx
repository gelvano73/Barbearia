/**
 * Layout do portal do barbeiro.
 * Navegação para agenda, horários, férias, comissões, meta e perfil.
 */
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { temRecurso } from '../data/planos'
import AppShell from './AppShell'

/** === Helpers === */
const linkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'active' : undefined)

export default function BarbeiroLayout() {
  const { auth, logout } = useAuth()
  const podeComissoes = temRecurso(auth?.plano as string | undefined, 'COMISSOES')

  return (
    <AppShell
      brandTitle="BARBA"
      brandSubtitle="BARBEIRO"
      nav={
        <>
          {/* === Navegação principal === */}
          <NavLink to="/barbeiro" end className={linkClass}>Dashboard</NavLink>
          <NavLink to="/barbeiro/agenda" className={linkClass}>Agenda</NavLink>
          <NavLink to="/barbeiro/horarios" className={linkClass}>Horários</NavLink>
          <NavLink to="/barbeiro/ferias" className={linkClass}>Férias</NavLink>
          <NavLink to="/barbeiro/historico" className={linkClass}>Histórico</NavLink>
          {podeComissoes && <NavLink to="/barbeiro/comissoes" className={linkClass}>Comissões</NavLink>}
          <NavLink to="/barbeiro/avaliacoes" className={linkClass}>Avaliações</NavLink>
          <NavLink to="/barbeiro/meta" className={linkClass}>Meta</NavLink>
          <NavLink to="/barbeiro/perfil" className={linkClass}>Perfil</NavLink>
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
