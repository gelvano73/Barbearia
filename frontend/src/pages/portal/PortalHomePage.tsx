/**
 * Home do portal do cliente com atalhos para agendar e fidelidade.
 */
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { temRecurso } from '../../data/planos'

export default function PortalHomePage() {
  const { auth } = useAuth()
  const podeFidelidade = temRecurso(auth?.plano as string | undefined, 'FIDELIDADE')

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Olá, {auth?.nome?.split(' ')[0]}</h1>
          <p>Bem-vindo ao portal de {auth?.nomeBarbearia}</p>
        </div>
      </div>
      {/* === Atalhos === */}
      <div className="panel" style={{ display: 'grid', gap: '1rem' }}>
        <p style={{ margin: 0, color: 'var(--muted)' }}>
          Agende um horário, acompanhe seus atendimentos e avalie seus barbeiros.
        </p>
        <div className="actions-row">
          <Link className="btn" to="/portal/agendar">Agendar</Link>
          <Link className="btn secondary" to="/portal/assistente">IA Atendimento</Link>
          <Link className="btn secondary" to="/portal/agendamentos">Meus horários</Link>
          <Link className="btn secondary" to="/portal/historico">Histórico</Link>
{podeFidelidade && <Link className="btn secondary" to="/portal/fidelidade">Fidelidade</Link>}
        </div>
      </div>
    </>
  )
}
