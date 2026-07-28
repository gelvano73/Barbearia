/**
 * Home da recepção com atalhos para fila, agenda e caixa.
 */
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

const atalhos = [
  { to: '/recepcao/clientes', titulo: 'Clientes', desc: 'Cadastro' },
  { to: '/recepcao/agenda', titulo: 'Agenda', desc: 'Manual' },
  { to: '/recepcao/fila', titulo: 'Fila', desc: 'Walk-in' },
  { to: '/recepcao/pagamentos', titulo: 'Pagamentos', desc: 'Receber' },
  { to: '/recepcao/caixa', titulo: 'Caixa', desc: 'Diário' },
]

export default function RecepcaoHomePage() {
  const { auth } = useAuth()

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Recepção</h1>
          <p>
            Olá, {auth?.nome}. Atendimento do dia em {auth?.nomeBarbearia}.
          </p>
        </div>
      </div>

      <div
        className="panel"
        style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))' }}
      >
        {atalhos.map((a) => (
          <Link key={a.to} to={a.to} style={{ display: 'block' }}>
            <div className="subtitle">{a.desc}</div>
            <strong style={{ fontSize: '1.4rem' }}>{a.titulo}</strong>
          </Link>
        ))}
      </div>
    </>
  )
}
