package com.barbearia.saas.controller;

import com.barbearia.saas.dto.agendamento.AgendamentoRequest;
import com.barbearia.saas.dto.agendamento.AgendamentoResponse;
import com.barbearia.saas.dto.agendamento.AtualizarStatusRequest;
import com.barbearia.saas.dto.barbeiro.BarbeiroResponse;
import com.barbearia.saas.dto.cliente.ClienteRequest;
import com.barbearia.saas.dto.cliente.ClienteResponse;
import com.barbearia.saas.dto.pagamento.PagamentoRequest;
import com.barbearia.saas.dto.pagamento.PagamentoResponse;
import com.barbearia.saas.dto.recepcao.*;
import com.barbearia.saas.dto.servico.ServicoResponse;
import com.barbearia.saas.service.PortalRecepcaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** API do portal da recepção: fila de atendimento e operações do dia. */
@RestController
@RequestMapping("/api/recepcao")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ATENDENTE', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Recepção", description = "Portal da recepcionista: agenda, fila, pagamentos e caixa")
public class PortalRecepcaoController {

    private final PortalRecepcaoService portalRecepcaoService;

    /** === Clientes e agendamentos === */

    /** Listar clientes. */
    @GetMapping("/clientes")
    @Operation(summary = "Listar clientes")
    public ResponseEntity<List<ClienteResponse>> clientes() {
        return ResponseEntity.ok(portalRecepcaoService.listarClientes());
    }

    /** Cadastrar cliente. */
    @PostMapping("/clientes")
    @Operation(summary = "Cadastrar cliente")
    public ResponseEntity<ClienteResponse> criarCliente(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalRecepcaoService.criarCliente(request));
    }

    /** Atualizar cliente. */
    @PutMapping("/clientes/{id}")
    @Operation(summary = "Atualizar cliente")
    public ResponseEntity<ClienteResponse> atualizarCliente(
            @PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(portalRecepcaoService.atualizarCliente(id, request));
    }

    /** Listar barbeiros ativos. */
    @GetMapping("/barbeiros")
    @Operation(summary = "Listar barbeiros ativos")
    public ResponseEntity<List<BarbeiroResponse>> barbeiros() {
        return ResponseEntity.ok(portalRecepcaoService.listarBarbeiros());
    }

    /** Listar serviços ativos. */
    @GetMapping("/servicos")
    @Operation(summary = "Listar serviços ativos")
    public ResponseEntity<List<ServicoResponse>> servicos() {
        return ResponseEntity.ok(portalRecepcaoService.listarServicos());
    }

    /** Listar agendamentos (manual). */
    @GetMapping("/agendamentos")
    @Operation(summary = "Listar agendamentos (manual)")
    public ResponseEntity<List<AgendamentoResponse>> agendamentos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(portalRecepcaoService.listarAgendamentos(data));
    }

    /** Agendamento manual. */
    @PostMapping("/agendamentos")
    @Operation(summary = "Agendamento manual")
    public ResponseEntity<AgendamentoResponse> criarAgendamento(@Valid @RequestBody AgendamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalRecepcaoService.criarAgendamento(request));
    }

    /** Atualizar status do agendamento. */
    @PatchMapping("/agendamentos/{id}/status")
    @Operation(summary = "Atualizar status do agendamento")
    public ResponseEntity<AgendamentoResponse> statusAgendamento(
            @PathVariable Long id, @Valid @RequestBody AtualizarStatusRequest request) {
        return ResponseEntity.ok(portalRecepcaoService.atualizarStatusAgendamento(id, request.getStatus()));
    }

    /** === Fila === */

    /** Listar fila de atendimento. */
    @GetMapping("/fila")
    @Operation(summary = "Listar fila de atendimento")
    public ResponseEntity<List<FilaResponse>> fila() {
        return ResponseEntity.ok(portalRecepcaoService.listarFila());
    }

    /** Inclui o cliente na fila de atendimento. */
    @PostMapping("/fila")
    @Operation(summary = "Adicionar cliente à fila")
    public ResponseEntity<FilaResponse> adicionarFila(@Valid @RequestBody FilaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalRecepcaoService.adicionarFila(request));
    }

    /** Atualizar status da fila. */
    @PatchMapping("/fila/{id}/status")
    @Operation(summary = "Atualizar status da fila")
    public ResponseEntity<FilaResponse> statusFila(
            @PathVariable Long id, @Valid @RequestBody AtualizarFilaStatusRequest request) {
        return ResponseEntity.ok(portalRecepcaoService.atualizarStatusFila(id, request.getStatus()));
    }

    /** === Caixa === */

    /** Retorna o caixa aberto (ou atual) da unidade. */
    @GetMapping("/caixa")
    @Operation(summary = "Caixa diário atual")
    public ResponseEntity<CaixaResponse> caixaAtual() {
        return ResponseEntity.ok(portalRecepcaoService.caixaAtual());
    }

    /** Abrir caixa diário. */
    @PostMapping("/caixa/abrir")
    @Operation(summary = "Abrir caixa diário")
    public ResponseEntity<CaixaResponse> abrirCaixa(@Valid @RequestBody AbrirCaixaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalRecepcaoService.abrirCaixa(request));
    }

    /** Fechar caixa diário. */
    @PostMapping("/caixa/fechar")
    @Operation(summary = "Fechar caixa diário")
    public ResponseEntity<CaixaResponse> fecharCaixa(@Valid @RequestBody FecharCaixaRequest request) {
        return ResponseEntity.ok(portalRecepcaoService.fecharCaixa(request));
    }

    /** Registra uma sangria (retirada) no caixa. */
    @PostMapping("/caixa/sangria")
    @Operation(summary = "Registrar sangria")
    public ResponseEntity<CaixaResponse> sangria(@Valid @RequestBody MovimentoCaixaRequest request) {
        return ResponseEntity.ok(portalRecepcaoService.sangria(request));
    }

    /** Registra um suprimento (entrada) no caixa. */
    @PostMapping("/caixa/suprimento")
    @Operation(summary = "Registrar suprimento")
    public ResponseEntity<CaixaResponse> suprimento(@Valid @RequestBody MovimentoCaixaRequest request) {
        return ResponseEntity.ok(portalRecepcaoService.suprimento(request));
    }

    /** === Pagamentos === */

    /** Listar pagamentos do dia. */
    @GetMapping("/pagamentos")
    @Operation(summary = "Listar pagamentos do dia")
    public ResponseEntity<List<PagamentoResponse>> pagamentos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(portalRecepcaoService.listarPagamentos(data));
    }

    /** Registrar pagamento. */
    @PostMapping("/pagamentos")
    @Operation(summary = "Registrar pagamento")
    public ResponseEntity<PagamentoResponse> registrarPagamento(@Valid @RequestBody PagamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalRecepcaoService.registrarPagamento(request));
    }
}
