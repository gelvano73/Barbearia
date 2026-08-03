/**
 * Catálogo comercial dos planos SaaS (para UI e divulgação).
 * Os valores devem bater com AssinaturaService.precoMensal no backend.
 */

export type PlanoId = 'TRIAL' | 'BASIC' | 'PRO' | 'ENTERPRISE'

export type PlanoCatalogo = {
  id: PlanoId
  nome: string
  precoMensal: number
  precoLabel: string
  destaque?: boolean
  descricao: string
  limites: string[]
  recursos: string[]
}

export const PLANOS_PAGOS: PlanoCatalogo[] = [
  {
    id: 'BASIC',
    nome: 'Basic',
    precoMensal: 97.9,
    precoLabel: 'R$ 97,90/mês',
    descricao: 'Para barbearia iniciante organizar agenda e caixa.',
    limites: [
      '1 unidade',
      'Até 3 barbeiros',
      'Até 500 clientes ativos',
      '1 usuário de recepção',
    ],
    recursos: [
      'Agenda e agendamentos',
      'Clientes, serviços e caixa',
      'Portal do barbeiro',
      'Portal da recepção',
      'Recibos e relatórios básicos',
      'Suporte por e-mail (útil)',
    ],
  },
  {
    id: 'PRO',
    nome: 'Pro',
    precoMensal: 197.9,
    precoLabel: 'R$ 197,90/mês',
    destaque: true,
    descricao: 'O mais escolhido: pagamentos online, WhatsApp e fidelidade.',
    limites: [
      'Até 3 unidades',
      'Até 10 barbeiros',
      'Clientes ilimitados',
      'Até 3 usuários de recepção',
    ],
    recursos: [
      'Tudo do Basic',
      'Mercado Pago / Pix online',
      'WhatsApp IA (número da plataforma)',
      'Programa de fidelidade',
      'Estoque e comissões',
      'Check-in e marketplace',
      'Backup sob demanda',
      'Suporte prioritário (chat/e-mail)',
    ],
  },
  {
    id: 'ENTERPRISE',
    nome: 'Enterprise',
    precoMensal: 397.9,
    precoLabel: 'R$ 397,90/mês',
    descricao: 'Multiunidades, NFS-e e operação avançada.',
    limites: [
      'Unidades ilimitadas',
      'Barbeiros ilimitados',
      'Clientes ilimitados',
      'Recepção ilimitada',
    ],
    recursos: [
      'Tudo do Pro',
      'NFS-e (Focus NFe)',
      'Franquias / multiunidades',
      'IA de gestão',
      'Onboarding assistido',
      'Suporte prioritário + implantação',
    ],
  },
]

export const TRIAL_INFO = {
  id: 'TRIAL' as const,
  nome: 'Trial',
  descricao: '14 dias grátis para testar o sistema completo.',
  precoLabel: 'Grátis por 14 dias',
}

/** Taxa opcional de implantação (fora da mensalidade). */
export const TAXA_IMPLANTACAO_LABEL = 'R$ 497 (opcional)'
