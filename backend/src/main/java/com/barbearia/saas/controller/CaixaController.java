package com.barbearia.saas.controller;

import com.barbearia.saas.dto.recepcao.AbrirCaixaRequest;
import com.barbearia.saas.dto.recepcao.CaixaResponse;
import com.barbearia.saas.dto.recepcao.FecharCaixaRequest;
import com.barbearia.saas.dto.recepcao.MovimentoCaixaRequest;
import com.barbearia.saas.service.CaixaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints REST de abertura, fechamento e movimentos de caixa. */
@RestController
@RequestMapping("/api/caixa")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Caixa", description = "Caixa diário: abrir, fechar, sangria e suprimento")
public class CaixaController {

    private final CaixaService caixaService;

    /** Caixa aberto atual. */
    @GetMapping
    @Operation(summary = "Caixa aberto atual")
    public ResponseEntity<CaixaResponse> atual() {
        return ResponseEntity.ok(caixaService.caixaAtual());
    }

    /** Lista o histórico de registros do módulo. */
    @GetMapping("/historico")
    @Operation(summary = "Histórico recente de caixas")
    public ResponseEntity<List<CaixaResponse>> historico() {
        return ResponseEntity.ok(caixaService.historico());
    }

    /** Abre o caixa do dia. */
    @PostMapping("/abrir")
    @Operation(summary = "Abrir caixa")
    public ResponseEntity<CaixaResponse> abrir(@Valid @RequestBody AbrirCaixaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caixaService.abrir(request));
    }

    /** Fecha o caixa do dia. */
    @PostMapping("/fechar")
    @Operation(summary = "Fechar caixa")
    public ResponseEntity<CaixaResponse> fechar(@Valid @RequestBody FecharCaixaRequest request) {
        return ResponseEntity.ok(caixaService.fechar(request));
    }

    /** Registra uma sangria (retirada) no caixa. */
    @PostMapping("/sangria")
    @Operation(summary = "Registrar sangria")
    public ResponseEntity<CaixaResponse> sangria(@Valid @RequestBody MovimentoCaixaRequest request) {
        return ResponseEntity.ok(caixaService.sangria(request));
    }

    /** Registra um suprimento (entrada) no caixa. */
    @PostMapping("/suprimento")
    @Operation(summary = "Registrar suprimento")
    public ResponseEntity<CaixaResponse> suprimento(@Valid @RequestBody MovimentoCaixaRequest request) {
        return ResponseEntity.ok(caixaService.suprimento(request));
    }
}
