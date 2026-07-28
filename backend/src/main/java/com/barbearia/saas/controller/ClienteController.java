package com.barbearia.saas.controller;

import com.barbearia.saas.dto.cliente.ClienteRequest;
import com.barbearia.saas.dto.cliente.ClienteResponse;
import com.barbearia.saas.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Endpoints REST para CRUD de clientes da barbearia. */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Clientes", description = "CRUD de clientes da barbearia")
public class ClienteController {

    private final ClienteService clienteService;

    /** Listar clientes. */
    @GetMapping
    @Operation(summary = "Listar clientes")
    public ResponseEntity<List<ClienteResponse>> listar(
            @RequestParam(defaultValue = "true") boolean apenasAtivos) {
        return ResponseEntity.ok(clienteService.listar(apenasAtivos));
    }

    /** Buscar cliente por ID. */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public ResponseEntity<ClienteResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    /** Cadastrar cliente. */
    @PostMapping
    @Operation(summary = "Cadastrar cliente")
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criar(request));
    }

    /** Atualizar cliente. */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente")
    public ResponseEntity<ClienteResponse> atualizar(
            @PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(id, request));
    }

    /** Upload de foto do cliente. */
    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de foto do cliente")
    public ResponseEntity<ClienteResponse> uploadFoto(
            @PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(clienteService.uploadFoto(id, arquivo));
    }

    /** Desativar cliente (soft delete). */
    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar cliente (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        clienteService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
