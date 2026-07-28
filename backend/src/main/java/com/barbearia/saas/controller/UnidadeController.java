package com.barbearia.saas.controller;

import com.barbearia.saas.dto.unidade.UnidadeRequest;
import com.barbearia.saas.dto.unidade.UnidadeResponse;
import com.barbearia.saas.service.UnidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints de gestão de unidades físicas da barbearia/franquia. */
@RestController
@RequestMapping("/api/unidades")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Unidades", description = "Filiais/lojas da barbearia")
public class UnidadeController {

    private final UnidadeService unidadeService;

    /** Listar unidades. */
    @GetMapping
    @Operation(summary = "Listar unidades")
    public ResponseEntity<List<UnidadeResponse>> listar(
            @RequestParam(defaultValue = "true") boolean apenasAtivos) {
        return ResponseEntity.ok(unidadeService.listar(apenasAtivos));
    }

    /** Buscar unidade por ID. */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar unidade por ID")
    public ResponseEntity<UnidadeResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(unidadeService.buscarPorId(id));
    }

    /** Cadastrar unidade. */
    @PostMapping
    @Operation(summary = "Cadastrar unidade")
    public ResponseEntity<UnidadeResponse> criar(@Valid @RequestBody UnidadeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unidadeService.criar(request));
    }

    /** Atualizar unidade. */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar unidade")
    public ResponseEntity<UnidadeResponse> atualizar(
            @PathVariable Long id, @Valid @RequestBody UnidadeRequest request) {
        return ResponseEntity.ok(unidadeService.atualizar(id, request));
    }

    /** Desativar unidade (soft delete). */
    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar unidade (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        unidadeService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
