/**
 * Política de Privacidade pública do Barba SaaS (LGPD).
 * Página acessível sem login, com texto legal alinhado aos dados tratados pelo sistema.
 */
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

const ATUALIZADO_EM = '2 de agosto de 2026 (NFS-e)'
const STORAGE_KEY = 'barba_aceite_privacidade'

export default function PrivacidadePage() {
  const [aceito, setAceito] = useState(false)
  const [marcado, setMarcado] = useState(false)
  const [msg, setMsg] = useState('')

  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) {
        setAceito(true)
        setMarcado(true)
      }
    } catch {
      /* ignore */
    }
  }, [])

  // Registra o aceite local do visitante nesta política
  const confirmarAceite = () => {
    if (!marcado) {
      setMsg('Marque a opção para confirmar o aceite.')
      return
    }
    const registro = {
      aceitoEm: new Date().toISOString(),
      versao: ATUALIZADO_EM,
    }
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(registro))
    } catch {
      /* ignore */
    }
    setAceito(true)
    setMsg('Aceite registrado neste dispositivo.')
  }

  return (
    <div className="legal-page">
      <div className="legal-doc">
        <p className="legal-brand">BARBA SAAS</p>
        <h1>Política de Privacidade</h1>
        <p className="legal-meta">Última atualização: {ATUALIZADO_EM}</p>

        <p>
          Esta Política de Privacidade descreve como a plataforma <strong>Barba SaaS</strong>
          (“Plataforma”, “nós”) e as barbearias que a utilizam (“Barbearia”, “Controlador”)
          tratam dados pessoais, em conformidade com a Lei nº 13.709/2018 (LGPD) e demais
          normas aplicáveis no Brasil.
        </p>

        <section>
          <h2>1. Quem somos e papéis no tratamento</h2>
          <p>
            O Barba SaaS é um software multiempresa para gestão de barbearias (agendamentos,
            clientes, pagamentos, estoque, fidelidade, marketplace, canais digitais e recursos
            de IA).
          </p>
          <ul>
            <li>
              <strong>Barbearia (Controlador):</strong> decide as finalidades do tratamento dos
              dados de seus clientes, barbeiros e atendentes no dia a dia da operação.
            </li>
            <li>
              <strong>Barba SaaS (Operador / provedor da plataforma):</strong> processa dados em
              nome da Barbearia para disponibilizar o sistema, hospedagem, autenticação, backups
              e funcionalidades técnicas.
            </li>
          </ul>
          <p>
            Para exercer direitos LGPD sobre dados de um estabelecimento específico, o titular
            deve, em regra, contatar a Barbearia em que se cadastrou. Questões sobre a
            infraestrutura da Plataforma podem ser encaminhadas ao canal indicado na seção 12.
          </p>
        </section>

        <section>
          <h2>2. Quais dados coletamos</h2>
          <p>Conforme o uso da Plataforma, podemos tratar as seguintes categorias:</p>

          <h3>2.1 Dados de cadastro e conta</h3>
          <ul>
            <li>
              Nome, e-mail real e ativo, CPF, telefone e senha (hash BCrypt). E-mails
              temporários/descartáveis não são aceitos; o domínio pode ser verificado via DNS.
            </li>
            <li>Dados da barbearia: nome, telefone, CNPJ (quando informado) e unidades.</li>
            <li>Papel de acesso: administrador, atendente, barbeiro ou cliente.</li>
            <li>Foto de perfil (cliente e barbeiro), quando enviada voluntariamente.</li>
            <li>
              Códigos temporários de acesso (OTP) enviados ao telefone cadastrado para autenticação
              alternativa, com validade limitada.
            </li>
          </ul>

          <h3>2.2 Dados operacionais da barbearia</h3>
          <ul>
            <li>Agendamentos, serviços, observações, status e histórico de atendimento.</li>
            <li>Pagamentos, caixa, comissões e registros financeiros relacionados ao serviço.</li>
            <li>Programa de fidelidade (pontos, resgates e movimentações).</li>
            <li>Estoque e produtos; pedidos do marketplace (nome, contato e itens do pedido).</li>
            <li>Avaliações, metas, horários de trabalho e solicitações de férias/folgas.</li>
          </ul>

          <h3>2.3 Dados biométricos / reconhecimento facial (quando habilitado)</h3>
          <ul>
            <li>
              Imagem facial e/ou assinatura digital derivada da imagem para cadastro e check-in
              do cliente.
            </li>
            <li>
              Trata-se de <strong>dado pessoal sensível</strong> (art. 5º, II, e art. 11 da LGPD).
              O uso depende de funcionalidade ativada pela Barbearia e de base legal adequada
              (em especial consentimento do titular, salvo hipótese legal específica).
            </li>
          </ul>

          <h3>2.4 Canais digitais e IA</h3>
          <ul>
            <li>
              Mensagens e identificadores de WhatsApp (número, conteúdo da conversa e sessão)
              quando a Barbearia integra o atendimento via webhook/API.
            </li>
            <li>
              Interações com assistentes de IA (texto enviado e respostas geradas) para
              agendamento ou apoio à gestão.
            </li>
            <li>
              Previsões de faturamento/estoque geradas a partir de dados operacionais já
              existentes (não exigem novos dados sensíveis além dos já tratados).
            </li>
          </ul>

          <h3>2.5 Dados técnicos</h3>
          <ul>
            <li>
              Tokens de autenticação (JWT) armazenados localmente no navegador para manter a
              sessão.
            </li>
            <li>
              Logs de aplicação, endereço IP e metadados técnicos eventualmente registrados
              para segurança, diagnóstico e auditoria.
            </li>
            <li>
              Contadores de tentativas de login e bloqueios temporários para prevenção de
              ataques de força bruta.
            </li>
          </ul>
          <h3>2.6 Dados fiscais (NFS-e)</h3>
          <ul>
            <li>
              CPF do tomador (cliente) e CNPJ/inscrição municipal do prestador (barbearia),
              necessários à emissão de Nota Fiscal de Serviços Eletrônica conforme regras da
              Receita Federal e legislação municipal do ISS.
            </li>
            <li>
              Dados da nota (número, código de verificação, valor, discriminação do serviço e
              status de autorização junto ao provedor/prefeitura).
            </li>
            <li>
              O CPF informado deve ser válido pelos dígitos verificadores oficiais e
              corresponder ao titular real do serviço — CPFs fictícios ou de demonstração
              não são aceitos para emissão.
            </li>
          </ul>
        </section>

        <section>
          <h2>2A. Segurança da informação</h2>
          <p>Adotamos medidas técnicas e organizacionais proporcionais, incluindo:</p>
          <ul>
            <li>Senhas com política de complexidade e armazenamento em hash (BCrypt).</li>
            <li>Login por e-mail ou CPF, com opção de código temporário no telefone cadastrado.</li>
            <li>Limitação de requisições (rate limit) e bloqueio após tentativas inválidas.</li>
            <li>Uso de consultas parametrizadas (proteção contra SQL Injection).</li>
            <li>Cabeçalhos HTTP de segurança (anti-clickjacking, MIME sniffing, HSTS quando HTTPS).</li>
            <li>Comunicação com a API preferencialmente via HTTPS em produção.</li>
          </ul>
          <p>
            Nenhum sistema é 100% invulnerável. Em caso de incidente relevante, adotaremos
            medidas de contenção e comunicação conforme a LGPD.
          </p>
        </section>

        <section>
          <h2>3. Finalidades do tratamento</h2>
          <p>Os dados são tratados para:</p>
          <ul>
            <li>Criar e autenticar contas; controlar acessos por perfil.</li>
            <li>Gerir agenda, fila, atendimentos, pagamentos, estoque e relatórios.</li>
            <li>Operar portal do cliente, painel do barbeiro e recepção.</li>
            <li>Permitir venda de produtos online (marketplace) e gestão de franquias/unidades.</li>
            <li>Realizar check-in por reconhecimento facial, quando autorizado.</li>
            <li>Atender clientes via WhatsApp e assistentes de IA, quando habilitados.</li>
            <li>Cumprir obrigações legais e fiscais (incluindo emissão de NFS-e quando habilitada).</li>
            <li>Melhorar a experiência e a estabilidade do serviço (suporte e correção de falhas).</li>
          </ul>
        </section>

        <section>
          <h2>4. Bases legais (LGPD)</h2>
          <p>Conforme o caso, o tratamento pode se fundamentar em:</p>
          <ul>
            <li>
              <strong>Execução de contrato / procedimentos preliminares</strong> (art. 7º, V) —
              cadastro, agendamento, pagamento e uso da conta.
            </li>
            <li>
              <strong>Legítimo interesse</strong> (art. 7º, IX) — segurança, prevenção a abuso,
              métricas internas e melhorias, com respeito aos direitos do titular.
            </li>
            <li>
              <strong>Cumprimento de obrigação legal ou regulatória</strong> (art. 7º, II).
            </li>
            <li>
              <strong>Consentimento</strong> (art. 7º, I e art. 11, I) — especialmente para
              foto/reconhecimento facial, comunicações opcionais e certos usos de canais digitais.
            </li>
          </ul>
          <p>
            O consentimento pode ser revogado a qualquer momento, sem afetar a licitude do
            tratamento anterior.
          </p>
        </section>

        <section>
          <h2>5. Compartilhamento de dados</h2>
          <p>Podemos compartilhar dados apenas quando necessário:</p>
          <ul>
            <li>
              Com a própria Barbearia e seus usuários autorizados (admin, recepção, barbeiros)
              no escopo multi-tenant da conta.
            </li>
            <li>
              Com provedores de infraestrutura (hospedagem, banco de dados, e-mail, WhatsApp/Meta
              Business, armazenamento de arquivos), na qualidade de operadores/suboperadores.
            </li>
            <li>
              Com autoridades públicas, mediante obrigação legal ou ordem válida.
            </li>
          </ul>
          <p>
            Não vendemos dados pessoais. Em grupos de franquia/multiempresa, o acesso entre
            unidades/empresas ocorre conforme a configuração feita pelo administrador responsável.
          </p>
        </section>

        <section>
          <h2>6. Transferência internacional</h2>
          <p>
            Se algum provedor de nuvem, API de mensagens ou ferramenta de suporte estiver fora do
            Brasil, a transferência observará a LGPD (cláusulas contratuais, países com grau
            adequado de proteção ou outra garantia legal aplicável).
          </p>
        </section>

        <section>
          <h2>7. Armazenamento, retenção e segurança</h2>
          <ul>
            <li>
              Dados ficam armazenados em banco de dados e, quando houver upload, em diretório/serviço
              de arquivos da Plataforma.
            </li>
            <li>
              Senhas não são armazenadas em texto claro (hash). Sessões usam tokens assinados.
            </li>
            <li>
              Podem existir rotinas de backup para continuidade do serviço.
            </li>
            <li>
              Conservamos os dados pelo tempo necessário às finalidades, à relação com a
              Barbearia e a obrigações legais/defesa de direitos. Após isso, eliminamos ou
              anonimizamos, quando tecnicamente viável.
            </li>
            <li>
              Adotamos medidas técnicas e administrativas razoáveis (controle de acesso, HTTPS
              em produção, segregação por barbearia). Nenhum sistema é 100% isento de riscos.
            </li>
          </ul>
        </section>

        <section>
          <h2>8. Cookies e armazenamento local</h2>
          <p>
            A Plataforma utiliza principalmente armazenamento local do navegador (por exemplo,
            token JWT) para manter o login. Não dependemos de cookies de publicidade de terceiros
            para o funcionamento essencial. Ferramentas de análise, se forem introduzidas no
            futuro, serão informadas nesta Política.
          </p>
        </section>

        <section>
          <h2>9. Direitos do titular</h2>
          <p>Nos termos da LGPD, você pode solicitar:</p>
          <ul>
            <li>Confirmação de tratamento e acesso aos dados.</li>
            <li>Correção de dados incompletos, inexatos ou desatualizados.</li>
            <li>Anonimização, bloqueio ou eliminação de dados desnecessários ou excessivos.</li>
            <li>Portabilidade, quando aplicável.</li>
            <li>Informação sobre compartilhamentos.</li>
            <li>Revogação do consentimento e oposição a tratamentos em bases cabíveis.</li>
            <li>Revisão de decisões automatizadas que afetem seus interesses, quando houver.</li>
          </ul>
          <p>
            Para dados tratados pela Barbearia (agendamentos, foto facial do estabelecimento etc.),
            solicite preferencialmente à própria Barbearia. A Plataforma poderá auxiliar o
            controlador no atendimento da solicitação.
          </p>
        </section>

        <section>
          <h2>10. Crianças e adolescentes</h2>
          <p>
            A Plataforma não é direcionada a menores de 18 anos sem supervisão. Cadastros de
            menores devem observar o consentimento específico de pelo menos um dos pais ou
            responsável legal, na forma da LGPD.
          </p>
        </section>

        <section>
          <h2>11. Alterações desta Política</h2>
          <p>
            Podemos atualizar este documento para refletir mudanças legais ou de produto. A data
            de “Última atualização” será revisada. Em alterações relevantes, poderemos destacar o
            aviso na própria Plataforma.
          </p>
        </section>

        <section>
          <h2>12. Contato e encarregado (DPO)</h2>
          <p>
            Para dúvidas sobre esta Política ou solicitações relacionadas à LGPD no âmbito da
            Plataforma Barba SaaS:
          </p>
          <ul>
            <li>
              E-mail: <a href="mailto:privacidade@barbasaas.local">privacidade@barbasaas.local</a>
              {' '}(substitua pelo canal oficial da sua operação).
            </li>
            <li>
              Titulares clientes de uma barbearia específica devem também contatar o
              estabelecimento onde o atendimento foi realizado.
            </li>
          </ul>
          <p>
            Você pode apresentar reclamação à Autoridade Nacional de Proteção de Dados (ANPD),
            se entender que seus direitos não foram atendidos.
          </p>
        </section>

        <section className="legal-aceite-box">
          <h2>13. Aceite</h2>
          <p>
            Ao marcar a opção abaixo, você declara que leu e compreendeu esta Política de
            Privacidade. Nos cadastros e no checkout da loja, o aceite também é exigido antes
            de concluir a operação.
          </p>
          <label className="aceite-privacidade" htmlFor="aceite-pagina-privacidade">
            <input
              id="aceite-pagina-privacidade"
              type="checkbox"
              checked={marcado}
              onChange={(e) => setMarcado(e.target.checked)}
              disabled={aceito}
            />
            <span>Li e aceito a Política de Privacidade do Barba SaaS.</span>
          </label>
          {msg && (
            <p className={aceito ? 'dashboard-ok' : 'error'} style={{ marginTop: '0.75rem' }}>
              {msg}
            </p>
          )}
          {!aceito ? (
            <button className="btn" type="button" style={{ marginTop: '0.9rem' }} onClick={confirmarAceite}>
              Confirmar aceite
            </button>
          ) : (
            <p className="dashboard-ok" style={{ marginTop: '0.75rem' }}>
              Aceite já confirmado neste dispositivo.
            </p>
          )}
        </section>

        <div className="legal-actions">
          <Link className="btn secondary" to="/">
            Início
          </Link>
          <Link className="btn secondary" to="/auth">
            Login admin
          </Link>
          <Link className="btn" to="/portal/login">
            Portal do cliente
          </Link>
        </div>
      </div>
    </div>
  )
}
