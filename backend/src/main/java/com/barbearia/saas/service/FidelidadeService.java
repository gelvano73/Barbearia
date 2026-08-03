package com.barbearia.saas.service;

import com.barbearia.saas.domain.enums.PlanoRecurso;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.TipoFidelidadeMovimento;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.fidelidade.*;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Programa de fidelidade: pontos, configuração, resgates e extrato. */
@Service
@RequiredArgsConstructor
public class FidelidadeService {

    private final PlanoAcessoService planoAcessoService;

    private final FidelidadeConfigRepository configRepository;
    private final FidelidadeSaldoRepository saldoRepository;
    private final FidelidadeMovimentoRepository movimentoRepository;
    private final ClienteRepository clienteRepository;
    private final BarbeariaRepository barbeariaRepository;

    /** === Configuração === */

    /** Retorna a configuração do programa de fidelidade. */
    @Transactional(readOnly = true)
    public FidelidadeConfigResponse getConfig() {
        return toConfigResponse(obterOuCriarConfig());
    }

    /** Atualiza a configuração do programa de fidelidade. */
    @Transactional
    public FidelidadeConfigResponse atualizarConfig(FidelidadeConfigRequest request) {
        planoAcessoService.exigirRecurso(PlanoRecurso.FIDELIDADE);
        FidelidadeConfig config = obterOuCriarConfig();
        config.setPontosPorAtendimento(request.getPontosPorAtendimento());
        config.setPontosParaResgate(request.getPontosParaResgate());
        config.setDescricao(request.getDescricao().trim());
        config.setAtivo(request.getAtivo());
        return toConfigResponse(configRepository.save(config));
    }

    /** === Saldos e painel === */

    /** Lista saldos. */
    @Transactional(readOnly = true)
    public List<FidelidadeSaldoResponse> listarSaldos() {
        FidelidadeConfig config = obterOuCriarConfig();
        return saldoRepository.findByBarbeariaIdOrderByPontosDesc(SecurityUtils.getBarbeariaIdAtual())
                .stream()
                .map(s -> toSaldoResponse(s, config))
                .toList();
    }

    /** Retorna o saldo de pontos de fidelidade do cliente. */
    @Transactional(readOnly = true)
    public FidelidadeSaldoResponse saldoCliente(Long clienteId) {
        FidelidadeConfig config = obterOuCriarConfig();
        Cliente cliente = encontrarCliente(clienteId);
        FidelidadeSaldo saldo = saldoRepository.findByClienteId(clienteId)
                .orElseGet(() -> FidelidadeSaldo.builder()
                        .cliente(cliente)
                        .barbearia(cliente.getBarbearia())
                        .pontos(0)
                        .pontosAcumulados(0)
                        .resgates(0)
                        .build());
        return toSaldoResponse(saldo, config);
    }

    /** Retorna o painel de fidelidade do cliente autenticado. */
    @Transactional(readOnly = true)
    public FidelidadeMeuPainelResponse meuPainel(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        Long barbeariaId = cliente.getBarbearia().getId();
        planoAcessoService.exigirRecurso(barbeariaId, PlanoRecurso.FIDELIDADE);
        FidelidadeConfig config = configRepository.findByBarbeariaId(barbeariaId)
                .orElseGet(() -> criarConfigPadrao(cliente.getBarbearia()));
        FidelidadeSaldo saldo = saldoRepository.findByClienteId(clienteId)
                .orElseGet(() -> FidelidadeSaldo.builder()
                        .cliente(cliente)
                        .barbearia(cliente.getBarbearia())
                        .pontos(0)
                        .pontosAcumulados(0)
                        .resgates(0)
                        .build());
        return FidelidadeMeuPainelResponse.builder()
                .config(toConfigResponse(config))
                .saldo(toSaldoResponse(saldo, config))
                .historico(movimentoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId).stream()
                        .map(this::toMovimentoResponse)
                        .toList())
                .build();
    }

    /** Lista o histórico de movimentos de fidelidade. */
    @Transactional(readOnly = true)
    public List<FidelidadeMovimentoResponse> historico(Long clienteId) {
        encontrarCliente(clienteId);
        return movimentoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId).stream()
                .map(this::toMovimentoResponse)
                .toList();
    }

    /** === Resgates e créditos === */

    /** Resgata pontos de fidelidade. */
    @Transactional
    public FidelidadeSaldoResponse resgatar(FidelidadeResgateRequest request) {
        planoAcessoService.exigirRecurso(PlanoRecurso.FIDELIDADE);
        FidelidadeConfig config = obterOuCriarConfig();
        if (!Boolean.TRUE.equals(config.getAtivo())) {
            throw new NegocioException("Programa de fidelidade inativo");
        }

        Cliente cliente = encontrarCliente(request.getClienteId());
        FidelidadeSaldo saldo = obterOuCriarSaldo(cliente);
        int custo = config.getPontosParaResgate();
        if (saldo.getPontos() < custo) {
            throw new NegocioException("Pontos insuficientes. Necessário: " + custo);
        }

        saldo.setPontos(saldo.getPontos() - custo);
        saldo.setResgates(saldo.getResgates() + 1);
        saldoRepository.save(saldo);

        String desc = request.getObservacao() != null && !request.getObservacao().isBlank()
                ? request.getObservacao().trim()
                : "Resgate: 1 corte grátis (" + custo + " pontos)";

        registrarMovimento(cliente, TipoFidelidadeMovimento.RESGATE, custo, saldo.getPontos(), desc, null);
        return toSaldoResponse(saldo, config);
    }

    /** Credita pontos ao concluir atendimento (idempotente por agendamento). */
    @Transactional
    public void creditarPorAgendamento(Agendamento agendamento) {
        if (agendamento == null || agendamento.getCliente() == null || agendamento.getBarbearia() == null) {
            return;
        }
        if (!planoAcessoService.temRecurso(agendamento.getBarbearia().getId(), PlanoRecurso.FIDELIDADE)) {
            return;
        }
        if (agendamento.getId() != null
                && movimentoRepository.existsByAgendamentoIdAndTipo(agendamento.getId(), TipoFidelidadeMovimento.CREDITO)) {
            return;
        }

        Long barbeariaId = agendamento.getBarbearia().getId();
        FidelidadeConfig config = configRepository.findByBarbeariaId(barbeariaId)
                .orElseGet(() -> criarConfigPadrao(agendamento.getBarbearia()));
        if (!Boolean.TRUE.equals(config.getAtivo())) {
            return;
        }

        Cliente cliente = agendamento.getCliente();
        FidelidadeSaldo saldo = saldoRepository.findByClienteId(cliente.getId())
                .orElseGet(() -> FidelidadeSaldo.builder()
                        .barbearia(agendamento.getBarbearia())
                        .cliente(cliente)
                        .pontos(0)
                        .pontosAcumulados(0)
                        .resgates(0)
                        .build());

        int pontos = config.getPontosPorAtendimento();
        saldo.setPontos(saldo.getPontos() + pontos);
        saldo.setPontosAcumulados(saldo.getPontosAcumulados() + pontos);
        saldoRepository.save(saldo);

        String servico = agendamento.getServico() != null ? agendamento.getServico() : "atendimento";
        registrarMovimento(
                cliente,
                TipoFidelidadeMovimento.CREDITO,
                pontos,
                saldo.getPontos(),
                "Pontos por " + servico,
                agendamento);
    }

    /** === Auxiliares === */

    private FidelidadeConfig obterOuCriarConfig() {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        return configRepository.findByBarbeariaId(barbeariaId)
                .orElseGet(() -> criarConfigPadrao(encontrarBarbearia()));
    }

    private FidelidadeConfig criarConfigPadrao(Barbearia barbearia) {
        return configRepository.save(FidelidadeConfig.builder()
                .barbearia(barbearia)
                .pontosPorAtendimento(1)
                .pontosParaResgate(10)
                .descricao("A cada 10 cortes = 1 grátis")
                .ativo(true)
                .build());
    }

    private FidelidadeSaldo obterOuCriarSaldo(Cliente cliente) {
        return saldoRepository.findByClienteId(cliente.getId())
                .orElseGet(() -> saldoRepository.save(FidelidadeSaldo.builder()
                        .barbearia(cliente.getBarbearia())
                        .cliente(cliente)
                        .pontos(0)
                        .pontosAcumulados(0)
                        .resgates(0)
                        .build()));
    }

    private void registrarMovimento(
            Cliente cliente,
            TipoFidelidadeMovimento tipo,
            int pontos,
            int saldoApos,
            String descricao,
            Agendamento agendamento) {
        movimentoRepository.save(FidelidadeMovimento.builder()
                .barbearia(cliente.getBarbearia())
                .cliente(cliente)
                .tipo(tipo)
                .pontos(pontos)
                .saldoApos(saldoApos)
                .descricao(descricao)
                .agendamento(agendamento)
                .build());
    }

    private Cliente encontrarCliente(Long clienteId) {
        return clienteRepository.findByIdAndBarbeariaId(clienteId, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
    }

    private Barbearia encontrarBarbearia() {
        return barbeariaRepository.findById(SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
    }

    private FidelidadeConfigResponse toConfigResponse(FidelidadeConfig c) {
        return FidelidadeConfigResponse.builder()
                .id(c.getId())
                .pontosPorAtendimento(c.getPontosPorAtendimento())
                .pontosParaResgate(c.getPontosParaResgate())
                .descricao(c.getDescricao())
                .ativo(c.getAtivo())
                .build();
    }

    private FidelidadeSaldoResponse toSaldoResponse(FidelidadeSaldo s, FidelidadeConfig config) {
        int meta = config.getPontosParaResgate();
        return FidelidadeSaldoResponse.builder()
                .clienteId(s.getCliente().getId())
                .clienteNome(s.getCliente().getNome())
                .pontos(s.getPontos())
                .pontosAcumulados(s.getPontosAcumulados())
                .resgates(s.getResgates())
                .pontosParaResgate(meta)
                .podeResgatar(Boolean.TRUE.equals(config.getAtivo()) && s.getPontos() >= meta)
                .build();
    }

    private FidelidadeMovimentoResponse toMovimentoResponse(FidelidadeMovimento m) {
        return FidelidadeMovimentoResponse.builder()
                .id(m.getId())
                .tipo(m.getTipo())
                .pontos(m.getPontos())
                .saldoApos(m.getSaldoApos())
                .descricao(m.getDescricao())
                .agendamentoId(m.getAgendamento() != null ? m.getAgendamento().getId() : null)
                .criadoEm(m.getCriadoEm())
                .build();
    }
}
