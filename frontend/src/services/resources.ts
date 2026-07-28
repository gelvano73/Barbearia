/**
 * Camada de recursos HTTP do frontend.
 * Agrupa endpoints por domínio (auth, clientes, agendamentos, portais, etc.).
 */
import api from './api'

/** Autenticação e cadastro (admin, cliente, barbeiro, recepção, OAuth). */
export const authApi = {
  login: (payload) => api.post('/auth/login', payload),
  registro: (payload) => api.post('/auth/registro', payload),
  loginCliente: (payload) => api.post('/auth/cliente/login', payload),
  registroCliente: (payload) => api.post('/auth/cliente/registro', payload),
  loginBarbeiro: (payload) => api.post('/auth/barbeiro/login', payload),
  loginRecepcao: (payload) => api.post('/auth/recepcao/login', payload),
  criarAtendente: (payload) => api.post('/auth/recepcao/atendente', payload),
  recuperarSenha: (payload) => api.post('/auth/recuperar-senha', payload),
  redefinirSenha: (payload) => api.post('/auth/redefinir-senha', payload),
  oauth: (provider, payload) => api.post(`/auth/oauth/${provider}`, payload),
  listarBarbearias: () => api.get('/auth/barbearias'),
}

/** CRUD de clientes no painel administrativo. */
export const clientesApi = {
  listar: (apenasAtivos = true) => api.get('/clientes', { params: { apenasAtivos } }),
  criar: (payload) => api.post('/clientes', payload),
  atualizar: (id, payload) => api.put(`/clientes/${id}`, payload),
  desativar: (id) => api.delete(`/clientes/${id}`),
  uploadFoto: (id, file) => {
    const form = new FormData()
    form.append('arquivo', file)
    return api.post(`/clientes/${id}/foto`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

/** CRUD de barbeiros, contas de acesso, metas e foto. */
export const barbeirosApi = {
  listar: (apenasAtivos = true) => api.get('/barbeiros', { params: { apenasAtivos } }),
  criar: (payload) => api.post('/barbeiros', payload),
  atualizar: (id, payload) => api.put(`/barbeiros/${id}`, payload),
  desativar: (id) => api.delete(`/barbeiros/${id}`),
  criarConta: (id, payload) => api.post(`/barbeiros/${id}/conta`, payload),
  definirMeta: (id, payload) => api.put(`/barbeiros/${id}/meta`, payload),
  uploadFoto: (id, file) => {
    const form = new FormData()
    form.append('arquivo', file)
    return api.post(`/barbeiros/${id}/foto`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

/** CRUD de serviços oferecidos pela barbearia. */
export const servicosApi = {
  listar: (apenasAtivos = true) => api.get('/servicos', { params: { apenasAtivos } }),
  criar: (payload) => api.post('/servicos', payload),
  atualizar: (id, payload) => api.put(`/servicos/${id}`, payload),
  desativar: (id) => api.delete(`/servicos/${id}`),
}

/** Agendamentos: listagem, horários livres, criação e status. */
export const agendamentosApi = {
  listar: (params = {}) => api.get('/agendamentos', { params }),
  horariosDisponiveis: (params = {}) => api.get('/agendamentos/horarios-disponiveis', { params }),
  criar: (payload) => api.post('/agendamentos', payload),
  atualizar: (id, payload) => api.put(`/agendamentos/${id}`, payload),
  atualizarStatus: (id, status) => api.patch(`/agendamentos/${id}/status`, { status }),
  cancelar: (id) => api.delete(`/agendamentos/${id}`),
}

/** Pagamentos registrados no painel administrativo. */
export const pagamentosApi = {
  listar: (params = {}) => api.get('/pagamentos', { params }),
  criar: (payload) => api.post('/pagamentos', payload),
  cancelar: (id) => api.delete(`/pagamentos/${id}`),
}

/** Programa de fidelidade: config, saldos, histórico e resgates. */
export const fidelidadeApi = {
  config: () => api.get('/fidelidade/config'),
  atualizarConfig: (payload) => api.put('/fidelidade/config', payload),
  saldos: () => api.get('/fidelidade/saldos'),
  saldo: (clienteId) => api.get(`/fidelidade/saldos/${clienteId}`),
  historico: (clienteId) => api.get(`/fidelidade/saldos/${clienteId}/historico`),
  resgatar: (payload) => api.post('/fidelidade/resgatar', payload),
}

/** Estoque: produtos e movimentações. */
export const estoqueApi = {
  produtos: (apenasAtivos = true) => api.get('/estoque/produtos', { params: { apenasAtivos } }),
  criarProduto: (payload) => api.post('/estoque/produtos', payload),
  atualizarProduto: (id, payload) => api.put(`/estoque/produtos/${id}`, payload),
  desativarProduto: (id) => api.delete(`/estoque/produtos/${id}`),
  movimentos: (produtoId) => api.get('/estoque/movimentos', { params: produtoId ? { produtoId } : {} }),
  movimentar: (payload) => api.post('/estoque/movimentos', payload),
}

/** Caixa: abertura, fechamento, sangria e suprimento. */
export const caixaApi = {
  atual: () => api.get('/caixa'),
  historico: () => api.get('/caixa/historico'),
  abrir: (payload) => api.post('/caixa/abrir', payload),
  fechar: (payload) => api.post('/caixa/fechar', payload),
  sangria: (payload) => api.post('/caixa/sangria', payload),
  suprimento: (payload) => api.post('/caixa/suprimento', payload),
}

/** Comissões de barbeiros (listagem e consolidado mensal). */
export const comissoesApi = {
  listar: (params = {}) => api.get('/comissoes', { params }),
  mensal: (params = {}) => api.get('/comissoes/mensal', { params }),
}

/** Relatórios gerenciais do painel. */
export const relatoriosApi = {
  gerar: (params = {}) => api.get('/relatorios', { params }),
}

/** Unidades/filiais da barbearia. */
export const unidadesApi = {
  listar: (apenasAtivos = true) => api.get('/unidades', { params: { apenasAtivos } }),
  criar: (payload) => api.post('/unidades', payload),
  atualizar: (id, payload) => api.put(`/unidades/${id}`, payload),
  desativar: (id) => api.delete(`/unidades/${id}`),
}

/** Integração WhatsApp / simulação de mensagens com IA. */
export const whatsappApi = {
  status: () => api.get('/whatsapp/status'),
  simular: (payload) => api.post('/whatsapp/simular', payload),
}

/** Previsões e insights de gestão com IA. */
export const gestaoApi = {
  previsoes: () => api.get('/gestao/previsoes'),
}

/** Check-in manual e facial de clientes. */
export const checkinApi = {
  hoje: () => api.get('/checkin/hoje'),
  manual: (clienteId) => api.post(`/checkin/manual/${clienteId}`),
  cadastrarFace: (clienteId, file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post(`/checkin/face/${clienteId}`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  facial: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/checkin/facial', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

/** Pedidos do marketplace (visão administrativa). */
export const marketplaceApi = {
  pedidos: () => api.get('/marketplace/pedidos'),
  atualizarStatus: (id, status) => api.patch(`/marketplace/pedidos/${id}/status`, { status }),
}

/** Marketplace público (loja sem autenticação). */
export const marketplacePublicApi = {
  produtos: (barbeariaId) => api.get(`/public/marketplace/${barbeariaId}/produtos`),
  criarPedido: (barbeariaId, payload) => api.post(`/public/marketplace/${barbeariaId}/pedidos`, payload),
}

/** Rede de franquias: visão, empresas e vínculos. */
export const franquiasApi = {
  visao: () => api.get('/franquias/visao'),
  empresas: () => api.get('/franquias/empresas'),
  criarEmpresa: (payload) => api.post('/franquias/empresas', payload),
  vincular: (empresaId) => api.post(`/franquias/empresas/${empresaId}/vincular`),
}

/** Portal do cliente: perfil, agenda, fidelidade e chat IA. */
export const portalApi = {
  perfil: () => api.get('/portal/perfil'),
  atualizarPerfil: (payload) => api.put('/portal/perfil', payload),
  uploadFoto: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/portal/perfil/foto', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  agendamentos: () => api.get('/portal/agendamentos'),
  historico: () => api.get('/portal/historico'),
  barbeiros: () => api.get('/portal/barbeiros'),
  servicos: () => api.get('/portal/servicos'),
  horariosDisponiveis: (params = {}) => api.get('/portal/horarios-disponiveis', { params }),
  agendar: (payload) => api.post('/portal/agendamentos', payload),
  reagendar: (id, payload) => api.patch(`/portal/agendamentos/${id}/reagendar`, payload),
  cancelar: (id) => api.delete(`/portal/agendamentos/${id}`),
  avaliar: (payload) => api.post('/portal/avaliacoes', payload),
  fidelidade: () => api.get('/portal/fidelidade'),
  iaChat: (payload) => api.post('/portal/ia/chat', payload),
}

/** Portal do barbeiro: agenda, horários, férias, comissões e meta. */
export const barbeiroPortalApi = {
  perfil: () => api.get('/barbeiro/perfil'),
  uploadFoto: (file) => {
    const form = new FormData()
    form.append('arquivo', file)
    return api.post('/barbeiro/perfil/foto', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  dashboard: () => api.get('/barbeiro/dashboard'),
  agenda: (data) => api.get('/barbeiro/agenda', { params: data ? { data } : {} }),
  historico: () => api.get('/barbeiro/historico'),
  atualizarStatus: (id, status) => api.patch(`/barbeiro/agendamentos/${id}/status`, { status }),
  horarios: () => api.get('/barbeiro/horarios'),
  salvarHorarios: (payload) => api.put('/barbeiro/horarios', payload),
  ferias: () => api.get('/barbeiro/ferias'),
  solicitarFerias: (payload) => api.post('/barbeiro/ferias', payload),
  cancelarFerias: (id) => api.delete(`/barbeiro/ferias/${id}`),
  comissoes: (params = {}) => api.get('/barbeiro/comissoes', { params }),
  avaliacoes: () => api.get('/barbeiro/avaliacoes'),
  meta: (params = {}) => api.get('/barbeiro/meta', { params }),
}

/** API da recepção: clientes, agenda, fila, pagamentos e caixa. */
export const recepcaoApi = {
  clientes: () => api.get('/recepcao/clientes'),
  criarCliente: (payload) => api.post('/recepcao/clientes', payload),
  atualizarCliente: (id, payload) => api.put(`/recepcao/clientes/${id}`, payload),
  barbeiros: () => api.get('/recepcao/barbeiros'),
  servicos: () => api.get('/recepcao/servicos'),
  agendamentos: (params = {}) => api.get('/recepcao/agendamentos', { params }),
  criarAgendamento: (payload) => api.post('/recepcao/agendamentos', payload),
  atualizarStatusAgendamento: (id, status) =>
    api.patch(`/recepcao/agendamentos/${id}/status`, { status }),
  fila: () => api.get('/recepcao/fila'),
  entrarFila: (payload) => api.post('/recepcao/fila', payload),
  atualizarFila: (id, status) => api.patch(`/recepcao/fila/${id}/status`, { status }),
  pagamentos: (params = {}) => api.get('/recepcao/pagamentos', { params }),
  criarPagamento: (payload) => api.post('/recepcao/pagamentos', payload),
  caixaAtual: () => api.get('/recepcao/caixa'),
  abrirCaixa: (payload) => api.post('/recepcao/caixa/abrir', payload),
  fecharCaixa: (payload) => api.post('/recepcao/caixa/fechar', payload),
  sangria: (payload) => api.post('/recepcao/caixa/sangria', payload),
  suprimento: (payload) => api.post('/recepcao/caixa/suprimento', payload),
}
