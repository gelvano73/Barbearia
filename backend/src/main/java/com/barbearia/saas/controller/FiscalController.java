package com.barbearia.saas.controller;

import com.barbearia.saas.dto.fiscal.ConfigFiscalRequest;
import com.barbearia.saas.dto.fiscal.ConfigFiscalResponse;
import com.barbearia.saas.dto.fiscal.NotaFiscalResponse;
import com.barbearia.saas.service.ConfigFiscalService;
import com.barbearia.saas.service.NotaFiscalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Configuração fiscal e emissão de NFS-e. */
@RestController
@RequestMapping("/api/fiscal")
@RequiredArgsConstructor
@Tag(name = "Fiscal / NFS-e", description = "Configuração do prestador e notas fiscais de serviço")
public class FiscalController {

    private final ConfigFiscalService configFiscalService;
    private final NotaFiscalService notaFiscalService;

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obter configuração fiscal da barbearia")
    public ResponseEntity<ConfigFiscalResponse> obterConfig() {
        return ResponseEntity.ok(configFiscalService.obter());
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Salvar configuração fiscal (CNPJ, IM, município, token)")
    public ResponseEntity<ConfigFiscalResponse> salvarConfig(@Valid @RequestBody ConfigFiscalRequest request) {
        return ResponseEntity.ok(configFiscalService.salvar(request));
    }

    @GetMapping("/notas")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    @Operation(summary = "Listar NFS-e emitidas")
    public ResponseEntity<List<NotaFiscalResponse>> listarNotas() {
        return ResponseEntity.ok(notaFiscalService.listar());
    }

    @GetMapping("/notas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    @Operation(summary = "Detalhe da NFS-e")
    public ResponseEntity<NotaFiscalResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(notaFiscalService.buscar(id));
    }

    @GetMapping("/notas/pagamento/{pagamentoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    @Operation(summary = "Buscar NFS-e pelo pagamento")
    public ResponseEntity<NotaFiscalResponse> porPagamento(@PathVariable Long pagamentoId) {
        return ResponseEntity.ok(notaFiscalService.porPagamento(pagamentoId));
    }

    @PostMapping("/notas/pagamento/{pagamentoId}/emitir")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    @Operation(summary = "Emitir NFS-e para um pagamento pago (CPF real do tomador)")
    public ResponseEntity<NotaFiscalResponse> emitir(@PathVariable Long pagamentoId) {
        return ResponseEntity.ok(notaFiscalService.emitirParaPagamento(pagamentoId));
    }

    @PostMapping("/notas/{id}/consultar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    @Operation(summary = "Consultar status no provedor Focus NFe")
    public ResponseEntity<NotaFiscalResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(notaFiscalService.consultarProvedor(id));
    }
}
