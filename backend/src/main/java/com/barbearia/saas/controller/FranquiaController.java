package com.barbearia.saas.controller;

import com.barbearia.saas.dto.franquia.EmpresaRequest;
import com.barbearia.saas.dto.franquia.EmpresaResponse;
import com.barbearia.saas.dto.franquia.FranquiaVisaoResponse;
import com.barbearia.saas.service.FranquiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints de visão consolidada de franquias e empresas multi-unidade. */
@RestController
@RequestMapping("/api/franquias")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Franquias", description = "Multiempresa e multiunidade")
public class FranquiaController {

    private final FranquiaService franquiaService;

    /** Visão da rede (empresa + unidades). */
    @GetMapping("/visao")
    @Operation(summary = "Visão da rede (empresa + unidades)")
    public ResponseEntity<FranquiaVisaoResponse> visao() {
        return ResponseEntity.ok(franquiaService.visaoRede());
    }

    /** Listar empresas (franqueadoras). */
    @GetMapping("/empresas")
    @Operation(summary = "Listar empresas (franqueadoras)")
    public ResponseEntity<List<EmpresaResponse>> empresas() {
        return ResponseEntity.ok(franquiaService.listarEmpresas());
    }

    /** Criar empresa franqueadora. */
    @PostMapping("/empresas")
    @Operation(summary = "Criar empresa franqueadora")
    public ResponseEntity<EmpresaResponse> criar(@Valid @RequestBody EmpresaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(franquiaService.criarEmpresa(request));
    }

    /** Vincular barbearia atual à empresa. */
    @PostMapping("/empresas/{empresaId}/vincular")
    @Operation(summary = "Vincular barbearia atual à empresa")
    public ResponseEntity<EmpresaResponse> vincular(@PathVariable Long empresaId) {
        return ResponseEntity.ok(franquiaService.vincularBarbeariaAtual(empresaId));
    }
}
