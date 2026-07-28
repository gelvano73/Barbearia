package com.barbearia.saas.controller;

import com.barbearia.saas.dto.fidelidade.*;
import com.barbearia.saas.service.FidelidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints do programa de fidelidade (config, saldo e resgates). */
@RestController
@RequestMapping("/api/fidelidade")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fidelidade", description = "Programa de fidelidade: pontos, resgates e histórico")
public class FidelidadeController {

    private final FidelidadeService fidelidadeService;

    /** Obter regra do programa. */
    @GetMapping("/config")
    @Operation(summary = "Obter regra do programa")
    public ResponseEntity<FidelidadeConfigResponse> config() {
        return ResponseEntity.ok(fidelidadeService.getConfig());
    }

    /** Atualiza a configuração do programa de fidelidade. */
    @PutMapping("/config")
    @Operation(summary = "Atualizar regra (ex.: a cada 10 cortes = 1 grátis)")
    public ResponseEntity<FidelidadeConfigResponse> atualizarConfig(@Valid @RequestBody FidelidadeConfigRequest request) {
        return ResponseEntity.ok(fidelidadeService.atualizarConfig(request));
    }

    /** Listar pontos dos clientes. */
    @GetMapping("/saldos")
    @Operation(summary = "Listar pontos dos clientes")
    public ResponseEntity<List<FidelidadeSaldoResponse>> saldos() {
        return ResponseEntity.ok(fidelidadeService.listarSaldos());
    }

    /** Saldo de um cliente. */
    @GetMapping("/saldos/{clienteId}")
    @Operation(summary = "Saldo de um cliente")
    public ResponseEntity<FidelidadeSaldoResponse> saldo(@PathVariable Long clienteId) {
        return ResponseEntity.ok(fidelidadeService.saldoCliente(clienteId));
    }

    /** Lista o histórico de registros do módulo. */
    @GetMapping("/saldos/{clienteId}/historico")
    @Operation(summary = "Histórico de pontos e resgates")
    public ResponseEntity<List<FidelidadeMovimentoResponse>> historico(@PathVariable Long clienteId) {
        return ResponseEntity.ok(fidelidadeService.historico(clienteId));
    }

    /** Resgatar corte grátis. */
    @PostMapping("/resgatar")
    @Operation(summary = "Resgatar corte grátis")
    public ResponseEntity<FidelidadeSaldoResponse> resgatar(@Valid @RequestBody FidelidadeResgateRequest request) {
        return ResponseEntity.ok(fidelidadeService.resgatar(request));
    }
}
