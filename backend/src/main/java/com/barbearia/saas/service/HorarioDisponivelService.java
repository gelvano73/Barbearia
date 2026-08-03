package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbeiro;
import com.barbearia.saas.domain.entity.BarbeiroHorario;
import com.barbearia.saas.domain.entity.Servico;
import com.barbearia.saas.domain.repository.BarbeiroHorarioRepository;
import com.barbearia.saas.domain.repository.BarbeiroRepository;
import com.barbearia.saas.domain.repository.ServicoRepository;
import com.barbearia.saas.dto.portal.HorarioDisponivelResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Cálculo de horários disponíveis para agendamento por barbeiro/data. */
@Service
@RequiredArgsConstructor
public class HorarioDisponivelService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter LABEL =
            DateTimeFormatter.ofPattern("EEE dd/MM HH:mm", Locale.forLanguageTag("pt-BR"));
    private static final LocalTime ABERTURA_PADRAO = LocalTime.of(9, 0);
    private static final LocalTime FECHAMENTO_PADRAO = LocalTime.of(18, 0);
    private static final int PASSO_MINUTOS = 30;

    private final BarbeiroRepository barbeiroRepository;
    private final BarbeiroHorarioRepository barbeiroHorarioRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoService agendamentoService;

    /** === Disponibilidade === */

    /** Lista os registros solicitados. */
    @Transactional(readOnly = true)
    public List<HorarioDisponivelResponse> listar(Long barbeiroId, Long servicoId, LocalDate data, int limite) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        int max = Math.min(Math.max(limite, 1), 100);

        List<Barbeiro> barbeiros = barbeiroRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId);
        if (barbeiroId != null) {
            barbeiros = barbeiros.stream().filter(b -> b.getId().equals(barbeiroId)).toList();
            if (barbeiros.isEmpty()) {
                throw new RecursoNaoEncontradoException("Barbeiro não encontrado");
            }
        }
        if (barbeiros.isEmpty()) {
            return List.of();
        }

        int duracao = 30;
        String servicoNome = null;
        if (servicoId != null) {
            Servico servico = servicoRepository.findByIdAndBarbeariaId(servicoId, barbeariaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));
            if (!Boolean.TRUE.equals(servico.getAtivo())) {
                throw new NegocioException("Serviço inativo");
            }
            duracao = servico.getDuracaoMinutos() != null ? servico.getDuracaoMinutos() : 30;
            servicoNome = servico.getNome();
        }

        LocalDateTime agora = LocalDateTime.now().plusMinutes(30).withSecond(0).withNano(0);
        int minute = agora.getMinute();
        if (minute % PASSO_MINUTOS != 0) {
            agora = agora.plusMinutes(PASSO_MINUTOS - (minute % PASSO_MINUTOS));
        }

        List<LocalDate> dias = new ArrayList<>();
        if (data != null) {
            dias.add(data);
        } else {
            for (int i = 0; i < 14; i++) {
                dias.add(agora.toLocalDate().plusDays(i));
            }
        }

        List<HorarioDisponivelResponse> livres = new ArrayList<>();
        for (LocalDate dia : dias) {
            if (livres.size() >= max) {
                break;
            }
            if (dia.getDayOfWeek() == DayOfWeek.SUNDAY && data == null) {
                continue;
            }
            for (Barbeiro barbeiro : barbeiros) {
                if (livres.size() >= max) {
                    break;
                }
                Intervalo expediente = expedienteDoDia(barbeiro.getId(), dia);
                if (expediente == null) {
                    continue;
                }
                LocalTime cursor = expediente.inicio();
                while (!cursor.plusMinutes(duracao).isAfter(expediente.fim()) && livres.size() < max) {
                    LocalDateTime slot = LocalDateTime.of(dia, cursor);
                    cursor = cursor.plusMinutes(PASSO_MINUTOS);
                    if (slot.isBefore(agora)) {
                        continue;
                    }
                    try {
                        agendamentoService.validarConflito(barbeiro.getId(), slot, duracao, null);
                        livres.add(HorarioDisponivelResponse.builder()
                                .dataHora(slot.format(ISO))
                                .label(formatarLabel(slot))
                                .barbeiroId(barbeiro.getId())
                                .barbeiroNome(barbeiro.getNome())
                                .servicoId(servicoId)
                                .servicoNome(servicoNome)
                                .build());
                    } catch (NegocioException ignored) {
                        // horário ocupado / conflito
                    }
                }
            }
        }
        return livres;
    }

    /** === Auxiliares === */

    private Intervalo expedienteDoDia(Long barbeiroId, LocalDate dia) {
        int diaSemana = dia.getDayOfWeek().getValue(); // 1=seg ... 7=dom
        BarbeiroHorario custom = barbeiroHorarioRepository
                .findByBarbeiroIdAndDiaSemana(barbeiroId, diaSemana)
                .orElse(null);
        if (custom != null) {
            if (!Boolean.TRUE.equals(custom.getAtivo())) {
                return null;
            }
            return new Intervalo(custom.getHoraInicio(), custom.getHoraFim());
        }
        if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return null;
        }
        return new Intervalo(ABERTURA_PADRAO, FECHAMENTO_PADRAO);
    }

    private String formatarLabel(LocalDateTime slot) {
        String raw = slot.format(LABEL);
        return raw.substring(0, 1).toUpperCase(Locale.forLanguageTag("pt-BR")) + raw.substring(1);
    }

    private record Intervalo(LocalTime inicio, LocalTime fim) {}
}
