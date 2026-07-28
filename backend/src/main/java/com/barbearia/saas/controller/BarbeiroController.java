package com.barbearia.saas.controller;

import com.barbearia.saas.dto.barbeiro.BarbeiroRequest;
import com.barbearia.saas.dto.barbeiro.BarbeiroResponse;
import com.barbearia.saas.service.BarbeiroService;
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

/** Endpoints REST para cadastro e gestão de barbeiros da barbearia. */
@RestController
@RequestMapping("/api/barbeiros")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Barbeiros", description = "CRUD de barbeiros da barbearia")
public class BarbeiroController {

    private final BarbeiroService barbeiroService;

    /** Listar barbeiros. */
    @GetMapping
    @Operation(summary = "Listar barbeiros")
    public ResponseEntity<List<BarbeiroResponse>> listar(
            @RequestParam(defaultValue = "true") boolean apenasAtivos) {
        return ResponseEntity.ok(barbeiroService.listar(apenasAtivos));
    }

    /** Buscar barbeiro por ID. */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar barbeiro por ID")
    public ResponseEntity<BarbeiroResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(barbeiroService.buscarPorId(id));
    }

    /** Cadastrar barbeiro. */
    @PostMapping
    @Operation(summary = "Cadastrar barbeiro")
    public ResponseEntity<BarbeiroResponse> criar(@Valid @RequestBody BarbeiroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(barbeiroService.criar(request));
    }

    /** Atualizar barbeiro. */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar barbeiro")
    public ResponseEntity<BarbeiroResponse> atualizar(
            @PathVariable Long id, @Valid @RequestBody BarbeiroRequest request) {
        return ResponseEntity.ok(barbeiroService.atualizar(id, request));
    }

    /** Upload de foto do barbeiro. */
    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de foto do barbeiro")
    public ResponseEntity<BarbeiroResponse> uploadFoto(
            @PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(barbeiroService.uploadFoto(id, arquivo));
    }

    /** Desativar barbeiro (soft delete). */
    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar barbeiro (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        barbeiroService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    /** Criar conta de acesso (role BARBEIRO). */
    @PostMapping("/{id}/conta")
    @Operation(summary = "Criar conta de acesso (role BARBEIRO)")
    public ResponseEntity<BarbeiroResponse> criarConta(
            @PathVariable Long id, @Valid @RequestBody com.barbearia.saas.dto.barbeiro.CriarContaBarbeiroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(barbeiroService.criarConta(id, request));
    }

    /** Definir meta mensal do barbeiro. */
    @PutMapping("/{id}/meta")
    @Operation(summary = "Definir meta mensal do barbeiro")
    public ResponseEntity<com.barbearia.saas.dto.portalbarbeiro.MetaProgressoResponse> definirMeta(
            @PathVariable Long id, @Valid @RequestBody com.barbearia.saas.dto.barbeiro.MetaRequest request) {
        return ResponseEntity.ok(barbeiroService.definirMeta(id, request));
    }
}
