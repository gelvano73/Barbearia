package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbeiro;
import com.barbearia.saas.domain.entity.Servico;
import com.barbearia.saas.domain.repository.BarbeiroRepository;
import com.barbearia.saas.domain.repository.ServicoRepository;
import com.barbearia.saas.dto.agendamento.AgendamentoResponse;
import com.barbearia.saas.dto.barbeiro.BarbeiroResponse;
import com.barbearia.saas.dto.ia.IaChatRequest;
import com.barbearia.saas.dto.ia.IaChatResponse;
import com.barbearia.saas.dto.ia.IaContexto;
import com.barbearia.saas.dto.portal.PortalAgendamentoRequest;
import com.barbearia.saas.dto.servico.ServicoResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Chat de atendimento assistido por IA com contexto da barbearia. */
@Service
@RequiredArgsConstructor
public class IaAtendimentoService {

    private static final DateTimeFormatter LABEL_FMT =
            DateTimeFormatter.ofPattern("EEE dd/MM HH:mm", new Locale("pt", "BR"));
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Pattern HORA_PATTERN =
            Pattern.compile("(?:às?\\s*)?(\\d{1,2})(?::(\\d{2}))?\\s*h?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_PATTERN =
            Pattern.compile("(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?");

    private final PortalClienteService portalClienteService;
    private final AgendamentoService agendamentoService;
    private final BarbeiroRepository barbeiroRepository;
    private final ServicoRepository servicoRepository;
    private final OpenAiClient openAiClient;

    /** === Chat === */

    /** Processa mensagem no chat de atendimento por IA. */
    @Transactional
    public IaChatResponse chat(IaChatRequest request) {
        String original = request.getMensagem() != null ? request.getMensagem().trim() : "";
        String msg = normalizar(original);
        IaContexto ctx = request.getContexto() != null ? request.getContexto() : new IaContexto();

        enriquecerContexto(msg, ctx);

        if (isConfirmacao(msg) && Boolean.TRUE.equals(ctx.getAguardandoConfirmacao()) && podeAgendar(ctx)) {
            return confirmarAgendamento(ctx, canalDe(request));
        }
        if (isNegacao(msg) && Boolean.TRUE.equals(ctx.getAguardandoConfirmacao())) {
            ctx.setAguardandoConfirmacao(false);
            ctx.setDataHora(null);
            return resposta("Sem problema. Me diga outro horário, serviço ou barbeiro.", "CANCELAR", ctx,
                    List.of("Sugerir serviços", "Horários disponíveis", "Quero agendar"));
        }

        // Prioridade: ações de agendar (botões "Agendar Corte", "Agendar sex. 17/07...")
        if (isIntencaoAgendar(msg, original)) {
            return fluxoAgendar(msg, ctx);
        }

        if (contemQualquer(msg, "ola", "oi", "bom dia", "boa tarde", "boa noite", "eai", "e ai")) {
            return saudacao(ctx);
        }
        if (contemQualquer(msg, "barbeiro", "profissional", "equipe", "quem atende", "ver barbeiros")) {
            return listarBarbeiros(ctx);
        }
        if (contemQualquer(msg, "horario", "disponivel", "vaga", "quando tem", "horarios disponiveis")) {
            return sugerirHorarios(ctx);
        }
        if (contemQualquer(msg, "servico", "servicos", "preco", "quanto custa", "suger", "recomend", "indicar")
                || (contemQualquer(msg, "barba", "corte", "sobrancelha", "hidrat", "pigment", "combo")
                && !contemQualquer(msg, "agend", "marcar", "reserv"))) {
            return sugerirServicos(msg, ctx);
        }
        if (contemQualquer(msg, "meu horario", "meus horario", "meus agendamento", "minha agenda")) {
            return meusAgendamentos(ctx);
        }
        if (contemQualquer(msg, "ajuda", "help", "o que voce", "como funciona")) {
            return ajuda(ctx);
        }

        Optional<String> llm = openAiClient.completar(
                "Você é a assistente de uma barbearia no Brasil. Responda em português, curto e útil. "
                        + "Foque em serviços, horários e agendamento. Não invente preços.",
                original.isBlank() ? msg : original);
        if (llm.isPresent()) {
            return resposta(llm.get(), "OPENAI", ctx,
                    List.of("Sugerir serviços", "Horários disponíveis", "Quero agendar", "Ver barbeiros"));
        }

        return resposta(
                "Posso sugerir serviços, mostrar barbeiros, indicar horários livres e agendar para você. "
                        + "Ex.: \"quero um corte amanhã às 15h\".",
                "FALLBACK",
                ctx,
                List.of("Sugerir serviços", "Horários disponíveis", "Quero agendar", "Ver barbeiros"));
    }

    /** === Intenções === */

    private boolean isIntencaoAgendar(String msg, String original) {
        if (msg.startsWith("agendar") || msg.startsWith("marcar") || msg.startsWith("reserv")) {
            return true;
        }
        return contemQualquer(msg, "quero agendar", "quero marcar", "quero um", "quero corte", "quero barba",
                "pode marcar", "fazer agendamento");
    }

    private IaChatResponse saudacao(IaContexto ctx) {
        String nome = SecurityUtils.getUsuarioAtual().getNome();
        String primeiro = nome != null && !nome.isBlank() ? nome.split(" ")[0] : "cliente";
        return resposta(
                "Olá, " + primeiro + "! Sou a assistente da barbearia. Posso sugerir serviços, "
                        + "mostrar horários e agendar seu atendimento.",
                "SAUDACAO",
                ctx,
                List.of("Sugerir serviços", "Horários disponíveis", "Quero agendar"));
    }

    private IaChatResponse ajuda(IaContexto ctx) {
        return resposta(
                "Como posso ajudar:\n"
                        + "• Sugerir serviços (ex.: \"quero barba\")\n"
                        + "• Listar barbeiros\n"
                        + "• Mostrar horários livres\n"
                        + "• Agendar (ex.: \"marcar corte amanhã às 15h\")",
                "AJUDA",
                ctx,
                List.of("Sugerir serviços", "Horários disponíveis", "Quero agendar"));
    }

    private IaChatResponse sugerirServicos(String msg, IaContexto ctx) {
        List<ServicoResponse> todos = portalClienteService.listarServicos();
        List<IaChatResponse.ServicoItem> sugeridos = ranquearServicos(msg, todos);

        if (sugeridos.isEmpty()) {
            sugeridos = todos.stream()
                    .limit(4)
                    .map(s -> IaChatResponse.ServicoItem.builder()
                            .id(s.getId())
                            .nome(s.getNome())
                            .descricao(s.getDescricao())
                            .preco(s.getPreco())
                            .duracaoMinutos(s.getDuracaoMinutos())
                            .motivo("Popular na casa")
                            .build())
                    .toList();
        }

        if (sugeridos.size() == 1) {
            ctx.setServicoId(sugeridos.get(0).getId());
        }

        StringBuilder sb = new StringBuilder("Sugestões para você:\n");
        for (IaChatResponse.ServicoItem s : sugeridos) {
            sb.append("• ").append(s.getNome())
                    .append(" — R$ ").append(s.getPreco())
                    .append(" (").append(s.getDuracaoMinutos()).append(" min)")
                    .append(s.getMotivo() != null ? " · " + s.getMotivo() : "")
                    .append("\n");
        }
        sb.append("Quer agendar algum? Digite o nome do serviço ou \"quero agendar\".");

        List<String> acoes = new ArrayList<>();
        sugeridos.forEach(s -> acoes.add("Agendar " + s.getNome()));
        acoes.add("Horários disponíveis");

        return IaChatResponse.builder()
                .resposta(sb.toString().trim())
                .intencao("SUGERIR_SERVICOS")
                .contexto(ctx)
                .servicosSugeridos(sugeridos)
                .acoesRapidas(acoes)
                .build();
    }

    private IaChatResponse listarBarbeiros(IaContexto ctx) {
        List<BarbeiroResponse> barbeiros = portalClienteService.listarBarbeiros();
        if (barbeiros.isEmpty()) {
            return resposta("Ainda não há barbeiros cadastrados. Fale com a recepção.", "BARBEIROS", ctx,
                    List.of("Sugerir serviços"));
        }
        StringBuilder sb = new StringBuilder("Nossa equipe:\n");
        List<String> acoes = new ArrayList<>();
        for (BarbeiroResponse b : barbeiros) {
            sb.append("• ").append(b.getNome());
            if (b.getEspecialidade() != null) {
                sb.append(" — ").append(b.getEspecialidade());
            }
            sb.append("\n");
            acoes.add("Quero com " + b.getNome());
        }
        sb.append("Diga com quem prefere, ou peça horários disponíveis.");
        acoes.add("Horários disponíveis");
        return resposta(sb.toString().trim(), "BARBEIROS", ctx, acoes);
    }

    private IaChatResponse sugerirHorarios(IaContexto ctx) {
        List<IaChatResponse.HorarioItem> slots = buscarHorariosLivres(ctx, 6);
        if (slots.isEmpty()) {
            return resposta(
                    "Não encontrei vagas nos próximos dias no horário comercial (9h–18h). "
                            + "Tente outro barbeiro ou serviço.",
                    "HORARIOS",
                    ctx,
                    List.of("Ver barbeiros", "Sugerir serviços"));
        }

        StringBuilder sb = new StringBuilder("Horários livres que encontrei:\n");
        List<String> acoes = new ArrayList<>();
        for (IaChatResponse.HorarioItem h : slots) {
            sb.append("• ").append(h.getLabel())
                    .append(" com ").append(h.getBarbeiroNome());
            if (h.getServicoNome() != null) {
                sb.append(" (").append(h.getServicoNome()).append(")");
            }
            sb.append("\n");
            acoes.add("Agendar " + h.getLabel());
        }
        sb.append("Responda com o horário desejado ou clique em uma sugestão.");

        return IaChatResponse.builder()
                .resposta(sb.toString().trim())
                .intencao("HORARIOS")
                .contexto(ctx)
                .horariosSugeridos(slots)
                .acoesRapidas(acoes)
                .build();
    }

    /** === Fluxo de agendamento === */

    private IaChatResponse fluxoAgendar(String msg, IaContexto ctx) {
        enriquecerContexto(msg, ctx);

        // "Agendar Corte" / "Agendar Sex 17/07 10:00"
        Matcher agendarNome = Pattern.compile("agendar\\s+(.+)", Pattern.CASE_INSENSITIVE).matcher(msg);
        if (agendarNome.find()) {
            String alvo = agendarNome.group(1).trim();
            aplicarAlvoAgendar(alvo, ctx);
        }

        if (msg.matches("(?i).*quero com\\s+.+")) {
            String nome = msg.replaceAll("(?i).*quero com\\s+", "").trim();
            resolverBarbeiroPorNome(nome).ifPresent(b -> ctx.setBarbeiroId(b.getId()));
        }

        if (!podeAgendar(ctx)) {
            if (ctx.getServicoId() == null) {
                return sugerirServicos(msg, ctx);
            }

            List<IaChatResponse.HorarioItem> slots = buscarHorariosLivres(ctx, 6);
            if (slots.isEmpty()) {
                return resposta(
                        "Serviço escolhido, mas não há horários livres nos próximos dias (9h–18h). "
                                + "Tente outro barbeiro ou volte mais tarde.",
                        "AGENDAR",
                        ctx,
                        List.of("Ver barbeiros", "Sugerir serviços"));
            }

            // Ainda falta horário/barbeiro: mostra opções em vez de voltar para a lista de serviços
            String servicoNome = nomeServico(ctx.getServicoId());
            StringBuilder sb = new StringBuilder("Ótimo, vamos agendar");
            if (servicoNome != null) {
                sb.append(" ").append(servicoNome);
            }
            sb.append(".\nEscolha um horário:\n");
            List<String> acoes = new ArrayList<>();
            for (IaChatResponse.HorarioItem h : slots) {
                sb.append("• ").append(h.getLabel()).append(" com ").append(h.getBarbeiroNome()).append("\n");
                acoes.add("Agendar " + h.getLabel());
            }

            return IaChatResponse.builder()
                    .resposta(sb.toString().trim())
                    .intencao("ESCOLHER_HORARIO")
                    .contexto(ctx)
                    .horariosSugeridos(slots)
                    .acoesRapidas(acoes)
                    .build();
        }

        return pedirConfirmacao(ctx, List.of());
    }

    private void aplicarAlvoAgendar(String alvo, IaContexto ctx) {
        String alvoNorm = normalizar(alvo);

        Optional<LocalDateTime> dt = parseDataHoraLivre(alvo);
        if (dt.isEmpty()) {
            // label tipo "sex. 17/07 10:00"
            Matcher m = Pattern.compile("(\\d{1,2})/(\\d{1,2}).*?(\\d{1,2}):(\\d{2})").matcher(alvo);
            if (m.find()) {
                int day = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int hour = Integer.parseInt(m.group(3));
                int min = Integer.parseInt(m.group(4));
                int year = LocalDate.now().getYear();
                LocalDate d = LocalDate.of(year, month, day);
                if (d.isBefore(LocalDate.now())) {
                    d = d.plusYears(1);
                }
                dt = Optional.of(LocalDateTime.of(d, LocalTime.of(hour, min)));
            }
        }
        if (dt.isPresent()) {
            ctx.setDataHora(dt.get().format(ISO_FMT));
            return;
        }

        portalClienteService.listarServicos().stream()
                .filter(s -> alvoNorm.contains(normalizar(s.getNome())) || normalizar(s.getNome()).contains(alvoNorm))
                .max(Comparator.comparingInt(s -> s.getNome().length()))
                .ifPresent(s -> ctx.setServicoId(s.getId()));
    }

    private IaChatResponse pedirConfirmacao(IaContexto ctx, List<IaChatResponse.HorarioItem> slots) {
        String servicoNome = nomeServico(ctx.getServicoId());
        String barbeiroNome = nomeBarbeiro(ctx.getBarbeiroId());
        String quando = formatarLabel(LocalDateTime.parse(ctx.getDataHora()));

        ctx.setAguardandoConfirmacao(true);
        String texto = "Posso confirmar:\n• Serviço: " + (servicoNome != null ? servicoNome : "padrão")
                + "\n• Barbeiro: " + barbeiroNome
                + "\n• Horário: " + quando
                + "\n\nResponda \"sim\" para confirmar ou \"não\" para cancelar.";

        return IaChatResponse.builder()
                .resposta(texto)
                .intencao("CONFIRMAR_AGENDAMENTO")
                .contexto(ctx)
                .horariosSugeridos(slots)
                .acoesRapidas(List.of("Sim, confirmar", "Não", "Outro horário"))
                .build();
    }

    private IaChatResponse confirmarAgendamento(IaContexto ctx, String canal) {
        try {
            PortalAgendamentoRequest req = new PortalAgendamentoRequest();
            req.setBarbeiroId(ctx.getBarbeiroId());
            req.setServicoId(ctx.getServicoId());
            req.setDataHora(LocalDateTime.parse(ctx.getDataHora()));
            req.setObservacoes("WHATSAPP".equalsIgnoreCase(canal)
                    ? "Agendado via WhatsApp (IA)"
                    : "Agendado via IA de atendimento");

            AgendamentoResponse ag = portalClienteService.agendar(req);
            ctx.setAguardandoConfirmacao(false);

            return IaChatResponse.builder()
                    .resposta("Pronto! Agendamento confirmado para "
                            + formatarLabel(ag.getDataHora())
                            + " com " + ag.getBarbeiroNome()
                            + (ag.getServico() != null ? " (" + ag.getServico() + ")" : "")
                            + ". Até lá!")
                    .intencao("AGENDADO")
                    .contexto(new IaContexto())
                    .agendamento(ag)
                    .acoesRapidas(List.of("Meus horários", "Sugerir serviços"))
                    .build();
        } catch (NegocioException e) {
            ctx.setAguardandoConfirmacao(false);
            ctx.setDataHora(null);
            return resposta(
                    "Não consegui agendar: " + e.getMessage() + ". Quer ver outros horários?",
                    "ERRO_AGENDAR",
                    ctx,
                    List.of("Horários disponíveis", "Ver barbeiros"));
        }
    }

    private IaChatResponse meusAgendamentos(IaContexto ctx) {
        var lista = portalClienteService.listarMeusAgendamentos().stream()
                .filter(a -> a.getStatus() != null
                        && !List.of("CANCELADO", "CONCLUIDO", "NAO_COMPARECEU").contains(a.getStatus().name()))
                .limit(5)
                .toList();
        if (lista.isEmpty()) {
            return resposta("Você não tem horários futuros. Quer que eu agende um?", "MEUS_HORARIOS", ctx,
                    List.of("Quero agendar", "Sugerir serviços"));
        }
        StringBuilder sb = new StringBuilder("Seus próximos horários:\n");
        for (var a : lista) {
            sb.append("• ").append(formatarLabel(a.getDataHora()))
                    .append(" — ").append(a.getBarbeiroNome())
                    .append(" (").append(a.getStatus()).append(")\n");
        }
        return resposta(sb.toString().trim(), "MEUS_HORARIOS", ctx, List.of("Quero agendar", "Sugerir serviços"));
    }

    /** === Contexto e busca === */

    private void enriquecerContexto(String msg, IaContexto ctx) {
        portalClienteService.listarServicos().stream()
                .filter(s -> msg.contains(normalizar(s.getNome())))
                .max(Comparator.comparingInt(s -> s.getNome().length()))
                .ifPresent(s -> ctx.setServicoId(s.getId()));

        // keywords
        if (ctx.getServicoId() == null) {
            if (contemQualquer(msg, "barba")) {
                acharServicoPorKeyword("barba").ifPresent(s -> ctx.setServicoId(s.getId()));
            } else if (contemQualquer(msg, "corte", "cabelo")) {
                acharServicoPorKeyword("corte").ifPresent(s -> ctx.setServicoId(s.getId()));
            } else if (contemQualquer(msg, "sobrancelha")) {
                acharServicoPorKeyword("sobrancelha").ifPresent(s -> ctx.setServicoId(s.getId()));
            } else if (contemQualquer(msg, "combo", "completo")) {
                acharServicoPorKeyword("completo").or(() -> acharServicoPorKeyword("combo"))
                        .ifPresent(s -> ctx.setServicoId(s.getId()));
            }
        }

        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        barbeiroRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId).stream()
                .filter(b -> msg.contains(normalizar(b.getNome())))
                .findFirst()
                .ifPresent(b -> ctx.setBarbeiroId(b.getId()));

        parseDataHoraLivre(msg).ifPresent(dt -> ctx.setDataHora(dt.format(ISO_FMT)));

        // slots label click: "agendar sex. 17/07 10:00"
        if (ctx.getDataHora() == null) {
            Matcher m = Pattern.compile("(\\d{1,2})/(\\d{1,2}).*?(\\d{1,2}):(\\d{2})").matcher(msg);
            if (m.find()) {
                int day = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int hour = Integer.parseInt(m.group(3));
                int min = Integer.parseInt(m.group(4));
                int year = LocalDate.now().getYear();
                LocalDate d = LocalDate.of(year, month, day);
                if (d.isBefore(LocalDate.now())) {
                    d = d.plusYears(1);
                }
                ctx.setDataHora(LocalDateTime.of(d, LocalTime.of(hour, min)).format(ISO_FMT));
            }
        }
    }

    private List<IaChatResponse.HorarioItem> buscarHorariosLivres(IaContexto ctx, int limite) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        List<Barbeiro> barbeiros = barbeiroRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId);
        if (ctx.getBarbeiroId() != null) {
            barbeiros = barbeiros.stream().filter(b -> b.getId().equals(ctx.getBarbeiroId())).toList();
        }
        if (barbeiros.isEmpty()) {
            return List.of();
        }

        int duracao = 30;
        String servicoNome = null;
        if (ctx.getServicoId() != null) {
            Servico s = servicoRepository.findByIdAndBarbeariaId(ctx.getServicoId(), barbeariaId).orElse(null);
            if (s != null) {
                duracao = s.getDuracaoMinutos() != null ? s.getDuracaoMinutos() : 30;
                servicoNome = s.getNome();
            }
        }

        List<IaChatResponse.HorarioItem> livres = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now().plusMinutes(30).withSecond(0).withNano(0);
        // arredonda para próximo slot de 30 min
        int minute = agora.getMinute();
        if (minute % 30 != 0) {
            agora = agora.plusMinutes(30 - (minute % 30));
        }

        for (int dia = 0; dia < 7 && livres.size() < limite; dia++) {
            LocalDate data = agora.toLocalDate().plusDays(dia);
            if (data.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            for (int h = 9; h < 18 && livres.size() < limite; h++) {
                for (int m = 0; m < 60 && livres.size() < limite; m += 30) {
                    LocalDateTime slot = LocalDateTime.of(data, LocalTime.of(h, m));
                    if (slot.isBefore(agora)) {
                        continue;
                    }
                    for (Barbeiro b : barbeiros) {
                        if (livres.size() >= limite) {
                            break;
                        }
                        try {
                            agendamentoService.validarConflito(b.getId(), slot, duracao, null);
                            livres.add(IaChatResponse.HorarioItem.builder()
                                    .dataHora(slot.format(ISO_FMT))
                                    .label(formatarLabel(slot))
                                    .barbeiroId(b.getId())
                                    .barbeiroNome(b.getNome())
                                    .servicoId(ctx.getServicoId())
                                    .servicoNome(servicoNome)
                                    .build());
                            break; // um barbeiro por slot basta
                        } catch (NegocioException ignored) {
                            // ocupado
                        }
                    }
                }
            }
        }
        return livres;
    }

    private List<IaChatResponse.ServicoItem> ranquearServicos(String msg, List<ServicoResponse> todos) {
        List<IaChatResponse.ServicoItem> ranked = new ArrayList<>();
        for (ServicoResponse s : todos) {
            String nome = normalizar(s.getNome());
            String desc = normalizar(s.getDescricao() != null ? s.getDescricao() : "");
            String motivo = null;
            int score = 0;
            if (msg.contains(nome)) {
                score += 10;
                motivo = "Combina com o que você pediu";
            }
            if (contemQualquer(msg, "barba") && nome.contains("barba")) {
                score += 8;
                motivo = "Ideal para barba";
            }
            if (contemQualquer(msg, "corte", "cabelo") && nome.contains("corte")) {
                score += 8;
                motivo = "Ideal para corte";
            }
            if (contemQualquer(msg, "sobrancelha") && nome.contains("sobrancelha")) {
                score += 8;
                motivo = "Para sobrancelha";
            }
            if (contemQualquer(msg, "barato", "econom", "promo") && s.getPreco() != null) {
                score += BigDecimal.valueOf(80).compareTo(s.getPreco()) >= 0 ? 3 : 0;
                motivo = "Bom custo-benefício";
            }
            if (contemQualquer(msg, "rapido", "rápido") && s.getDuracaoMinutos() != null && s.getDuracaoMinutos() <= 30) {
                score += 4;
                motivo = "Atendimento rápido";
            }
            if (contemQualquer(msg, "completo", "combo") && (nome.contains("completo") || nome.contains("combo"))) {
                score += 8;
                motivo = "Pacote completo";
            }
            if (score > 0) {
                ranked.add(IaChatResponse.ServicoItem.builder()
                        .id(s.getId())
                        .nome(s.getNome())
                        .descricao(s.getDescricao())
                        .preco(s.getPreco())
                        .duracaoMinutos(s.getDuracaoMinutos())
                        .motivo(motivo)
                        .build());
            }
        }
        ranked.sort(Comparator.comparing(IaChatResponse.ServicoItem::getPreco,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return ranked.stream().limit(4).toList();
    }

    /** === Resolução === */

    private Optional<Servico> acharServicoPorKeyword(String keyword) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        return servicoRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId).stream()
                .filter(s -> normalizar(s.getNome()).contains(keyword))
                .findFirst();
    }

    private Optional<Barbeiro> resolverBarbeiroPorNome(String nome) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        String n = normalizar(nome);
        return barbeiroRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId).stream()
                .filter(b -> normalizar(b.getNome()).contains(n) || n.contains(normalizar(b.getNome())))
                .findFirst();
    }

    private Optional<LocalDateTime> parseDataHoraLivre(String texto) {
        String t = normalizar(texto);
        LocalDate data = null;
        if (t.contains("hoje")) {
            data = LocalDate.now();
        } else if (t.contains("amanha") || t.contains("amanhã")) {
            data = LocalDate.now().plusDays(1);
        } else {
            for (DayOfWeek dow : DayOfWeek.values()) {
                String nome = dow.getDisplayName(java.time.format.TextStyle.FULL, new Locale("pt", "BR"));
                if (t.contains(normalizar(nome))) {
                    data = LocalDate.now();
                    while (data.getDayOfWeek() != dow) {
                        data = data.plusDays(1);
                    }
                    break;
                }
            }
        }

        Matcher dataM = DATA_PATTERN.matcher(texto);
        if (dataM.find()) {
            int day = Integer.parseInt(dataM.group(1));
            int month = Integer.parseInt(dataM.group(2));
            int year = dataM.group(3) != null
                    ? Integer.parseInt(dataM.group(3).length() == 2 ? "20" + dataM.group(3) : dataM.group(3))
                    : LocalDate.now().getYear();
            data = LocalDate.of(year, month, day);
            if (data.isBefore(LocalDate.now())) {
                data = data.plusYears(1);
            }
        }

        LocalTime hora = null;
        Matcher horaM = HORA_PATTERN.matcher(texto);
        while (horaM.find()) {
            int h = Integer.parseInt(horaM.group(1));
            int m = horaM.group(2) != null ? Integer.parseInt(horaM.group(2)) : 0;
            if (h >= 0 && h <= 23 && m >= 0 && m <= 59) {
                // evita capturar "17/07" como hora 17
                int start = horaM.start();
                if (start > 0 && texto.charAt(start - 1) == '/') {
                    continue;
                }
                hora = LocalTime.of(h, m);
            }
        }

        try {
            if (texto.contains("T") && texto.length() >= 16) {
                return Optional.of(LocalDateTime.parse(texto.substring(0, Math.min(19, texto.length()))));
            }
        } catch (DateTimeParseException ignored) {
        }

        if (data != null && hora != null) {
            return Optional.of(LocalDateTime.of(data, hora));
        }
        if (data != null && hora == null && (t.contains("amanha") || t.contains("hoje"))) {
            return Optional.empty(); // precisa hora
        }
        return Optional.empty();
    }

    /** === Auxiliares === */

    private boolean podeAgendar(IaContexto ctx) {
        return ctx.getBarbeiroId() != null && ctx.getDataHora() != null;
    }

    private boolean isConfirmacao(String msg) {
        return contemQualquer(msg, "sim", "confirmar", "confirma", "pode", "ok", "fechado", "isso", "quero sim");
    }

    private boolean isNegacao(String msg) {
        return contemQualquer(msg, "nao", "não", "cancelar", "outro horario", "outro horário");
    }

    private String nomeServico(Long id) {
        if (id == null) return null;
        return portalClienteService.listarServicos().stream()
                .filter(s -> s.getId().equals(id))
                .map(ServicoResponse::getNome)
                .findFirst()
                .orElse(null);
    }

    private String nomeBarbeiro(Long id) {
        if (id == null) return null;
        return portalClienteService.listarBarbeiros().stream()
                .filter(b -> b.getId().equals(id))
                .map(BarbeiroResponse::getNome)
                .findFirst()
                .orElse("barbeiro");
    }

    private String formatarLabel(LocalDateTime dt) {
        String s = dt.format(LABEL_FMT);
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private IaChatResponse resposta(String texto, String intencao, IaContexto ctx, List<String> acoes) {
        return IaChatResponse.builder()
                .resposta(texto)
                .intencao(intencao)
                .contexto(ctx)
                .acoesRapidas(acoes)
                .build();
    }

    private String normalizar(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private boolean contemQualquer(String msg, String... palavras) {
        for (String p : palavras) {
            if (msg.contains(normalizar(p))) {
                return true;
            }
        }
        return false;
    }

    private String canalDe(IaChatRequest request) {
        return request.getCanal() != null && !request.getCanal().isBlank()
                ? request.getCanal().trim().toUpperCase(Locale.ROOT)
                : "PORTAL";
    }
}
