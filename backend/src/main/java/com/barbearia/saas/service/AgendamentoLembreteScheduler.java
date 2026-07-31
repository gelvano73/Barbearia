package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Agendamento;
import com.barbearia.saas.domain.entity.NotificacaoLog;
import com.barbearia.saas.domain.enums.CanalNotificacao;
import com.barbearia.saas.domain.enums.StatusAgendamento;
import com.barbearia.saas.domain.enums.StatusNotificacao;
import com.barbearia.saas.domain.repository.AgendamentoRepository;
import com.barbearia.saas.domain.repository.NotificacaoLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Agendador que envia lembretes de agendamentos com data/hora nas próximas 24 horas. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgendamentoLembreteScheduler {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final AgendamentoRepository agendamentoRepository;
    private final NotificacaoLogRepository notificacaoLogRepository;
    private final NotificacaoService notificacaoService;

    /** Executa a varredura de agendamentos próximos e dispara os lembretes ainda não enviados. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void enviarLembretes() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime limite = agora.plusHours(24);

        List<Agendamento> proximos = agendamentoRepository
                .findByDataHoraBetweenAndStatus(agora, limite, StatusAgendamento.AGENDADO);

        for (Agendamento agendamento : proximos) {
            String referencia = "lembrete-agendamento-" + agendamento.getId();
            if (notificacaoLogRepository.existsByReferencia(referencia)) {
                continue;
            }

            try {
                enviarLembrete(agendamento);
                notificacaoLogRepository.save(NotificacaoLog.builder()
                        .barbeariaId(agendamento.getBarbearia().getId())
                        .canal(CanalNotificacao.WHATSAPP)
                        .destino(agendamento.getCliente().getTelefone())
                        .assunto("Lembrete de agendamento")
                        .status(StatusNotificacao.ENVIADO)
                        .referencia(referencia)
                        .build());
            } catch (Exception e) {
                log.error("Falha ao enviar lembrete do agendamento {}: {}", agendamento.getId(), e.getMessage(), e);
            }
        }
    }

    private void enviarLembrete(Agendamento agendamento) {
        String nomeServico = agendamento.getServico() != null && !agendamento.getServico().isBlank()
                ? agendamento.getServico()
                : (agendamento.getServicoRef() != null ? agendamento.getServicoRef().getNome() : "seu horário");

        String mensagem = "Olá, " + agendamento.getCliente().getNome() + "! "
                + "Lembrete: você tem " + nomeServico + " agendado com " + agendamento.getBarbeiro().getNome()
                + " em " + agendamento.getDataHora().format(FORMATO) + ".";

        notificacaoService.notificarCliente(
                agendamento.getCliente(), agendamento.getBarbearia().getId(), "Lembrete de agendamento", mensagem);
    }
}
