package com.barbearia.saas.controller;

import com.barbearia.saas.dto.servico.ServicoRequest;
import com.barbearia.saas.dto.servico.ServicoResponse;
import com.barbearia.saas.service.ServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints REST para catálogo de serviços oferecidos pela barbearia. */
@RestController
@RequestMapping("/api/servicos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Serviços", description = "CRUD de serviços da barbearia")
public class ServicoController {

    private final ServicoService servicoService;

    /** Listar serviços. */
    @GetMapping
    @Operation(summary = "Listar serviços")
    public ResponseEntity<List<ServicoResponse>> listar(
            @RequestParam(defaultValue = "true") boolean apenasAtivos) {
        return ResponseEntity.ok(servicoService.listar(apenasAtivos));
    }

    /** Buscar serviço por ID. */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar serviço por ID")
    public ResponseEntity<ServicoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(servicoService.buscarPorId(id));
    }

    /** Cadastrar serviço. */
    @PostMapping
    @Operation(summary = "Cadastrar serviço")
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicoService.criar(request));
    }

    /** Atualizar serviço. */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço")
    public ResponseEntity<ServicoResponse> atualizar(
            @PathVariable Long id, @Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.ok(servicoService.atualizar(id, request));
    }

    /** Desativar serviço (soft delete). */
    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar serviço (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        servicoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
