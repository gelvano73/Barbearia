/**
 * Define as rotas da aplicação e os guards por papel (admin, cliente, barbeiro, recepção).
 * Redireciona usuários não autenticados ou com role incompatível para o fluxo correto.
 */
import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Layout from './components/Layout'
import PortalLayout from './components/PortalLayout'
import BarbeiroLayout from './components/BarbeiroLayout'
import RecepcaoLayout from './components/RecepcaoLayout'
import AuthPage from './pages/AuthPage'
import RecuperarSenhaPage from './pages/RecuperarSenhaPage'
import DashboardPage from './pages/DashboardPage'
import ClientesPage from './pages/ClientesPage'
import BarbeirosPage from './pages/BarbeirosPage'
import ServicosPage from './pages/ServicosPage'
import AgendamentosPage from './pages/AgendamentosPage'
import PagamentosPage from './pages/PagamentosPage'
import FiscalPage from './pages/FiscalPage'
import FidelidadePage from './pages/FidelidadePage'
import EstoquePage from './pages/EstoquePage'
import CaixaPage from './pages/CaixaPage'
import ComissoesPage from './pages/ComissoesPage'
import RelatoriosPage from './pages/RelatoriosPage'
import UnidadesPage from './pages/UnidadesPage'
import WhatsappPage from './pages/WhatsappPage'
import GestaoIaPage from './pages/GestaoIaPage'
import CheckinPage from './pages/CheckinPage'
import MarketplacePage from './pages/MarketplacePage'
import LojaPublicaPage from './pages/LojaPublicaPage'
import FranquiasPage from './pages/FranquiasPage'
import AssinaturaPage from './pages/AssinaturaPage'
import PortalLoginPage from './pages/portal/PortalLoginPage'
import PortalRegistroPage from './pages/portal/PortalRegistroPage'
import PortalRecuperarSenhaPage from './pages/portal/PortalRecuperarSenhaPage'
import PortalHomePage from './pages/portal/PortalHomePage'
import PortalPerfilPage from './pages/portal/PortalPerfilPage'
import PortalAgendarPage from './pages/portal/PortalAgendarPage'
import PortalAgendamentosPage from './pages/portal/PortalAgendamentosPage'
import PortalHistoricoPage from './pages/portal/PortalHistoricoPage'
import PortalFidelidadePage from './pages/portal/PortalFidelidadePage'
import PortalIaPage from './pages/portal/PortalIaPage'
import BarbeiroLoginPage from './pages/barbeiro/BarbeiroLoginPage'
import BarbeiroDashboardPage from './pages/barbeiro/BarbeiroDashboardPage'
import BarbeiroAgendaPage from './pages/barbeiro/BarbeiroAgendaPage'
import BarbeiroHorariosPage from './pages/barbeiro/BarbeiroHorariosPage'
import BarbeiroFeriasPage from './pages/barbeiro/BarbeiroFeriasPage'
import BarbeiroHistoricoPage from './pages/barbeiro/BarbeiroHistoricoPage'
import BarbeiroComissoesPage from './pages/barbeiro/BarbeiroComissoesPage'
import BarbeiroAvaliacoesPage from './pages/barbeiro/BarbeiroAvaliacoesPage'
import BarbeiroMetaPage from './pages/barbeiro/BarbeiroMetaPage'
import BarbeiroPerfilPage from './pages/barbeiro/BarbeiroPerfilPage'
import RecepcaoLoginPage from './pages/recepcao/RecepcaoLoginPage'
import RecepcaoHomePage from './pages/recepcao/RecepcaoHomePage'
import RecepcaoClientesPage from './pages/recepcao/RecepcaoClientesPage'
import RecepcaoAgendaPage from './pages/recepcao/RecepcaoAgendaPage'
import RecepcaoFilaPage from './pages/recepcao/RecepcaoFilaPage'
import RecepcaoPagamentosPage from './pages/recepcao/RecepcaoPagamentosPage'
import RecepcaoCaixaPage from './pages/recepcao/RecepcaoCaixaPage'
import PrivacidadePage from './pages/PrivacidadePage'

/** === Guards de rota === */
function homeForRole(auth) {
  if (auth?.role === 'CLIENTE') return '/portal'
  if (auth?.role === 'BARBEIRO') return '/barbeiro'
  if (auth?.role === 'ATENDENTE') return '/recepcao'
  return '/'
}

function StaffRoute({ children }) {
  const { isAuthenticated, isCliente, isBarbeiro, isAtendente } = useAuth()
  if (!isAuthenticated) return <Navigate to="/auth" replace />
  if (isCliente) return <Navigate to="/portal" replace />
  if (isBarbeiro) return <Navigate to="/barbeiro" replace />
  if (isAtendente) return <Navigate to="/recepcao" replace />
  return children
}

function ClienteRoute({ children }) {
  const { isAuthenticated, isCliente, auth } = useAuth()
  if (!isAuthenticated) return <Navigate to="/portal/login" replace />
  if (!isCliente) return <Navigate to={homeForRole(auth)} replace />
  return children
}

function BarbeiroRoute({ children }) {
  const { isAuthenticated, isBarbeiro, auth } = useAuth()
  if (!isAuthenticated) return <Navigate to="/barbeiro/login" replace />
  if (!isBarbeiro) return <Navigate to={homeForRole(auth)} replace />
  return children
}

function RecepcaoRoute({ children }) {
  const { isAuthenticated, isAtendente, isAdmin, auth } = useAuth()
  if (!isAuthenticated) return <Navigate to="/recepcao/login" replace />
  if (!isAtendente && !isAdmin) return <Navigate to={homeForRole(auth)} replace />
  return children
}

export default function App() {
  return (
    <Routes>
      {/* === Rotas públicas === */}
      <Route path="/auth" element={<AuthPage />} />
      <Route path="/recuperar-senha" element={<RecuperarSenhaPage />} />
      <Route path="/privacidade" element={<PrivacidadePage />} />
      <Route path="/loja/:barbeariaId" element={<LojaPublicaPage />} />

      {/* === Auth dos portais === */}
      <Route path="/portal/login" element={<PortalLoginPage />} />
      <Route path="/portal/registro" element={<PortalRegistroPage />} />
      <Route path="/portal/recuperar-senha" element={<PortalRecuperarSenhaPage />} />
      <Route path="/barbeiro/login" element={<BarbeiroLoginPage />} />
      <Route path="/recepcao/login" element={<RecepcaoLoginPage />} />

      {/* === Portal do cliente === */}
      <Route
        path="/portal"
        element={
          <ClienteRoute>
            <PortalLayout />
          </ClienteRoute>
        }
      >
        <Route index element={<PortalHomePage />} />
        <Route path="perfil" element={<PortalPerfilPage />} />
        <Route path="agendar" element={<PortalAgendarPage />} />
        <Route path="assistente" element={<PortalIaPage />} />
        <Route path="agendamentos" element={<PortalAgendamentosPage />} />
        <Route path="historico" element={<PortalHistoricoPage />} />
        <Route path="fidelidade" element={<PortalFidelidadePage />} />
      </Route>

      {/* === Portal do barbeiro === */}
      <Route
        path="/barbeiro"
        element={
          <BarbeiroRoute>
            <BarbeiroLayout />
          </BarbeiroRoute>
        }
      >
        <Route index element={<BarbeiroDashboardPage />} />
        <Route path="agenda" element={<BarbeiroAgendaPage />} />
        <Route path="horarios" element={<BarbeiroHorariosPage />} />
        <Route path="ferias" element={<BarbeiroFeriasPage />} />
        <Route path="historico" element={<BarbeiroHistoricoPage />} />
        <Route path="comissoes" element={<BarbeiroComissoesPage />} />
        <Route path="avaliacoes" element={<BarbeiroAvaliacoesPage />} />
        <Route path="meta" element={<BarbeiroMetaPage />} />
        <Route path="perfil" element={<BarbeiroPerfilPage />} />
      </Route>

      {/* === Recepção === */}
      <Route
        path="/recepcao"
        element={
          <RecepcaoRoute>
            <RecepcaoLayout />
          </RecepcaoRoute>
        }
      >
        <Route index element={<RecepcaoHomePage />} />
        <Route path="clientes" element={<RecepcaoClientesPage />} />
        <Route path="agenda" element={<RecepcaoAgendaPage />} />
        <Route path="fila" element={<RecepcaoFilaPage />} />
        <Route path="pagamentos" element={<RecepcaoPagamentosPage />} />
        <Route path="caixa" element={<RecepcaoCaixaPage />} />
      </Route>

      {/* === Painel admin === */}
      <Route
        path="/"
        element={
          <StaffRoute>
            <Layout />
          </StaffRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="clientes" element={<ClientesPage />} />
        <Route path="barbeiros" element={<BarbeirosPage />} />
        <Route path="servicos" element={<ServicosPage />} />
        <Route path="agendamentos" element={<AgendamentosPage />} />
        <Route path="pagamentos" element={<PagamentosPage />} />
        <Route path="fiscal" element={<FiscalPage />} />
        <Route path="fidelidade" element={<FidelidadePage />} />
        <Route path="estoque" element={<EstoquePage />} />
        <Route path="caixa" element={<CaixaPage />} />
        <Route path="comissoes" element={<ComissoesPage />} />
        <Route path="relatorios" element={<RelatoriosPage />} />
        <Route path="unidades" element={<UnidadesPage />} />
        <Route path="whatsapp" element={<WhatsappPage />} />
        <Route path="gestao" element={<GestaoIaPage />} />
        <Route path="checkin" element={<CheckinPage />} />
        <Route path="marketplace" element={<MarketplacePage />} />
        <Route path="franquias" element={<FranquiasPage />} />
        <Route path="assinatura" element={<AssinaturaPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
