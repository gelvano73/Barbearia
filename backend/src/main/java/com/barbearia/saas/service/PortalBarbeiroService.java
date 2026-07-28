package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.StatusAgendamento;
import com.barbearia.saas.domain.enums.StatusFerias;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.agendamento.AgendamentoResponse;
import com.barbearia.saas.dto.portal.AvaliacaoResponse;
import com.barbearia.saas.dto.portalbarbeiro.*;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/** Regras do portal do barbeiro (dashboard, agenda, férias, metas). */
@Service
@RequiredArgsConstructor
public class PortalBarbeiroService {

    private final BarbeiroRepository barbeiroRepository;
    private final BarbeiroHorarioRepository horarioRepository;
    private final BarbeiroFeriasRepository feriasRepository;
    private final BarbeiroMetaRepository metaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ComissaoRepository comissaoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final AgendamentoService agendamentoService;
    private final ComissaoService comissaoService;
    private final FidelidadeService fidelidadeService;
    private final FotoStorageService fotoStorageService;

    /** Retorna o perfil do usuário autenticado. */
    @Transactional(readOnly = true)
    public BarbeiroPerfilResponse perfil() {
        return toPerfil(getBarbeiroAtual());
    }

    /** Faz upload e associa a foto ao registro. */
    @Transactional
    public BarbeiroPerfilResponse uploadFoto(MultipartFile arquivo) {
        Barbeiro barbeiro = getBarbeiroAtual();
        String url = fotoStorageService.salvar(arquivo, "barbeiros", "barbeiro-" + barbeiro.getId());
        barbeiro.setFotoUrl(url);
        return toPerfil(barbeiroRepository.save(barbeiro));
    }

    /** Retorna o painel consolidado do portal. */
    @Transactional(readOnly = true)
    public BarbeiroDashboardResponse dashboard() {
        Barbeiro barbeiro = getBarbeiroAtual();
        Long barbeiroId = barbeiro.getId();
        LocalDate hoje = LocalDate.now();
        YearMonth mes = YearMonth.now();
        LocalDateTime inicioMes = mes.atDay(1).atStartOfDay();
        LocalDateTime fimMes = mes.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime inicioHoje = hoje.atStartOfDay();
        LocalDateTime fimHoje = hoje.plusDays(1).atStartOfDay();

        long hojeCount = agendamentoRepository
                .findByBarbeiroIdAndDataHoraBetweenOrderByDataHoraAsc(barbeiroId, inicioHoje, fimHoje)
                .stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .count();

        long atendimentosMes = agendamentoRepository.countByBarbeiroIdAndStatusAndDataHoraBetween(
                barbeiroId, StatusAgendamento.CONCLUIDO, inicioMes, fimMes);

        BigDecimal comissaoMes = comissaoRepository.somarComissaoPeriodo(barbeiroId, inicioMes, fimMes);
        Double media = avaliacaoRepository.mediaPorBarbeiro(barbeiroId);
        long totalAvaliacoes = avaliacaoRepository.countByBarbeiroId(barbeiroId);

        List<String> proximos = agendamentoRepository
                .findByBarbeiroIdAndDataHoraBetweenOrderByDataHoraAsc(
                        barbeiroId, LocalDateTime.now(), LocalDateTime.now().plusDays(3))
                .stream()
                .filter(a -> a.getStatus() == StatusAgendamento.AGENDADO || a.getStatus() == StatusAgendamento.CONFIRMADO)
                .limit(5)
                .map(a -> a.getDataHora() + " - " + a.getCliente().getNome())
                .toList();

        return BarbeiroDashboardResponse.builder()
                .nome(barbeiro.getNome())
                .agendamentosHoje(hojeCount)
                .atendimentosMes(atendimentosMes)
                .comissaoMes(comissaoMes)
                .mediaAvaliacoes(media)
                .totalAvaliacoes(totalAvaliacoes)
                .meta(metaProgresso(mes.getYear(), mes.getMonthValue()))
                .proximosHorarios(proximos)
                .build();
    }

    /** Lista a agenda de atendimentos do barbeiro. */
    @Transactional(readOnly = true)
    public List<AgendamentoResponse> agenda(LocalDate data) {
        Long barbeiroId = SecurityUtils.getBarbeiroIdAtual();
        LocalDate dia = data != null ? data : LocalDate.now();
        return agendamentoRepository.findByBarbeiroIdAndDataHoraBetweenOrderByDataHoraAsc(
                        barbeiroId, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay())
                .stream()
                .map(agendamentoService::toResponse)
                .toList();
    }

    /** Lista o histórico de atendimentos do barbeiro. */
    @Transactional(readOnly = true)
    public List<AgendamentoResponse> historico() {
        Long barbeiroId = SecurityUtils.getBarbeiroIdAtual();
        return agendamentoRepository.findByBarbeiroIdAndStatusOrderByDataHoraDesc(
                        barbeiroId, StatusAgendamento.CONCLUIDO)
                .stream()
                .map(agendamentoService::toResponse)
                .toList();
    }

    /** Atualiza status. */
    @Transactional
    public AgendamentoResponse atualizarStatus(Long id, StatusAgendamento status) {
        Agendamento agendamento = agendamentoRepository.findByIdAndBarbeiroId(id, SecurityUtils.getBarbeiroIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado"));
        agendamento.setStatus(status);
        Agendamento salvo = agendamentoRepository.save(agendamento);
        if (status == StatusAgendamento.CONCLUIDO) {
            comissaoService.gerarSeNecessario(salvo);
            fidelidadeService.creditarPorAgendamento(salvo);
        }
        return agendamentoService.toResponse(salvo);
    }

    /** Lista horarios. */
    @Transactional(readOnly = true)
    public List<HorarioResponse> listarHorarios() {
        return horarioRepository.findByBarbeiroIdOrderByDiaSemanaAsc(SecurityUtils.getBarbeiroIdAtual())
                .stream()
                .map(this::toHorario)
                .toList();
    }

    /** Salva a grade de horários em lote. */
    @Transactional
    public List<HorarioResponse> salvarHorarios(HorariosBatchRequest request) {
        Barbeiro barbeiro = getBarbeiroAtual();
        for (HorariosBatchRequest.HorarioItem item : request.getHorarios()) {
            if (!item.getHoraFim().isAfter(item.getHoraInicio())) {
                throw new NegocioException("Hora fim deve ser após hora início");
            }
            BarbeiroHorario horario = horarioRepository
                    .findByBarbeiroIdAndDiaSemana(barbeiro.getId(), item.getDiaSemana())
                    .orElse(BarbeiroHorario.builder().barbeiro(barbeiro).diaSemana(item.getDiaSemana()).build());
            horario.setHoraInicio(item.getHoraInicio());
            horario.setHoraFim(item.getHoraFim());
            horario.setAtivo(item.getAtivo() == null || item.getAtivo());
            horarioRepository.save(horario);
        }
        return listarHorarios();
    }

    /** Lista ferias. */
    @Transactional(readOnly = true)
    public List<FeriasResponse> listarFerias() {
        return feriasRepository.findByBarbeiroIdOrderByDataInicioDesc(SecurityUtils.getBarbeiroIdAtual())
                .stream()
                .map(this::toFerias)
                .toList();
    }

    /** Solicita período de férias. */
    @Transactional
    public FeriasResponse solicitarFerias(FeriasRequest request) {
        if (request.getDataFim().isBefore(request.getDataInicio())) {
            throw new NegocioException("Data fim deve ser igual ou após data início");
        }
        BarbeiroFerias ferias = feriasRepository.save(BarbeiroFerias.builder()
                .barbeiro(getBarbeiroAtual())
                .dataInicio(request.getDataInicio())
                .dataFim(request.getDataFim())
                .motivo(blankToNull(request.getMotivo()))
                .status(StatusFerias.SOLICITADO)
                .build());
        return toFerias(ferias);
    }

    /** Cancela ferias. */
    @Transactional
    public void cancelarFerias(Long id) {
        BarbeiroFerias ferias = feriasRepository.findByIdAndBarbeiroId(id, SecurityUtils.getBarbeiroIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Férias não encontradas"));
        if (ferias.getStatus() == StatusFerias.APROVADO) {
            throw new NegocioException("Não é possível cancelar férias já aprovadas");
        }
        ferias.setStatus(StatusFerias.CANCELADO);
        feriasRepository.save(ferias);
    }

    /** Lista comissoes. */
    @Transactional(readOnly = true)
    public List<ComissaoResponse> listarComissoes(Integer ano, Integer mes) {
        YearMonth ym = ano != null && mes != null ? YearMonth.of(ano, mes) : YearMonth.now();
        LocalDateTime inicio = ym.atDay(1).atStartOfDay();
        LocalDateTime fim = ym.plusMonths(1).atDay(1).atStartOfDay();
        return comissaoRepository.findByBarbeiroIdAndCriadoEmBetweenOrderByCriadoEmDesc(
                        SecurityUtils.getBarbeiroIdAtual(), inicio, fim)
                .stream()
                .map(c -> ComissaoResponse.builder()
                        .id(c.getId())
                        .agendamentoId(c.getAgendamento().getId())
                        .clienteNome(c.getAgendamento().getCliente().getNome())
                        .servico(c.getAgendamento().getServico())
                        .valorServico(c.getValorServico())
                        .percentual(c.getPercentual())
                        .valorComissao(c.getValorComissao())
                        .criadoEm(c.getCriadoEm())
                        .build())
                .toList();
    }

    /** Lista avaliacoes. */
    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listarAvaliacoes() {
        return avaliacaoRepository.findByBarbeiroIdOrderByCriadoEmDesc(SecurityUtils.getBarbeiroIdAtual())
                .stream()
                .map(a -> AvaliacaoResponse.builder()
                        .id(a.getId())
                        .agendamentoId(a.getAgendamento().getId())
                        .barbeiroId(a.getBarbeiro().getId())
                        .barbeiroNome(a.getBarbeiro().getNome())
                        .nota(a.getNota())
                        .comentario(a.getComentario())
                        .criadoEm(a.getCriadoEm())
                        .build())
                .toList();
    }

    /** Retorna o progresso da meta do barbeiro. */
    @Transactional(readOnly = true)
    public MetaProgressoResponse metaProgresso(Integer ano, Integer mes) {
        YearMonth ym = ano != null && mes != null ? YearMonth.of(ano, mes) : YearMonth.now();
        Long barbeiroId = SecurityUtils.getBarbeiroIdAtual();
        LocalDateTime inicio = ym.atDay(1).atStartOfDay();
        LocalDateTime fim = ym.plusMonths(1).atDay(1).atStartOfDay();

        BarbeiroMeta meta = metaRepository.findByBarbeiroIdAndAnoAndMes(barbeiroId, ym.getYear(), ym.getMonthValue())
                .orElse(null);

        int metaAtendimentos = meta != null ? meta.getMetaAtendimentos() : 0;
        BigDecimal metaComissao = meta != null ? meta.getMetaComissao() : BigDecimal.ZERO;
        long realizados = agendamentoRepository.countByBarbeiroIdAndStatusAndDataHoraBetween(
                barbeiroId, StatusAgendamento.CONCLUIDO, inicio, fim);
        BigDecimal comissao = comissaoRepository.somarComissaoPeriodo(barbeiroId, inicio, fim);

        return MetaProgressoResponse.builder()
                .ano(ym.getYear())
                .mes(ym.getMonthValue())
                .metaAtendimentos(metaAtendimentos)
                .atendimentosRealizados(realizados)
                .metaComissao(metaComissao)
                .comissaoRealizada(comissao)
                .percentualAtendimentos(percentual(realizados, metaAtendimentos))
                .percentualComissao(percentual(comissao, metaComissao))
                .build();
    }

    private Double percentual(long valor, int meta) {
        if (meta <= 0) return 0.0;
        return BigDecimal.valueOf(valor * 100.0 / meta).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private Double percentual(BigDecimal valor, BigDecimal meta) {
        if (meta == null || meta.compareTo(BigDecimal.ZERO) <= 0) return 0.0;
        return valor.multiply(new BigDecimal("100"))
                .divide(meta, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Barbeiro getBarbeiroAtual() {
        return barbeiroRepository.findById(SecurityUtils.getBarbeiroIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbeiro não encontrado"));
    }

    private BarbeiroPerfilResponse toPerfil(Barbeiro barbeiro) {
        return BarbeiroPerfilResponse.builder()
                .id(barbeiro.getId())
                .nome(barbeiro.getNome())
                .telefone(barbeiro.getTelefone())
                .especialidade(barbeiro.getEspecialidade())
                .fotoUrl(barbeiro.getFotoUrl())
                .build();
    }

    private HorarioResponse toHorario(BarbeiroHorario h) {
        return HorarioResponse.builder()
                .id(h.getId())
                .diaSemana(h.getDiaSemana())
                .horaInicio(h.getHoraInicio())
                .horaFim(h.getHoraFim())
                .ativo(h.getAtivo())
                .build();
    }

    private FeriasResponse toFerias(BarbeiroFerias f) {
        return FeriasResponse.builder()
                .id(f.getId())
                .dataInicio(f.getDataInicio())
                .dataFim(f.getDataFim())
                .motivo(f.getMotivo())
                .status(f.getStatus())
                .criadoEm(f.getCriadoEm())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
