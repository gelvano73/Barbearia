package com.barbearia.saas.controller;

import com.barbearia.saas.dto.pagamento.PagamentoRequest;
import com.barbearia.saas.dto.pagamento.PagamentoResponse;
import com.barbearia.saas.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Endpoints de registro e consulta de pagamentos de serviços. */
@RestController
@RequestMapping("/api/pagamentos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pagamentos", description = "Registro de pagamentos (PIX, crédito, débito, dinheiro)")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    /** Listar pagamentos por data. */
    @GetMapping
    @Operation(summary = "Listar pagamentos por data")
    public ResponseEntity<List<PagamentoResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(pagamentoService.listar(data));
    }

    /** Buscar pagamento por ID. */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar pagamento por ID")
    public ResponseEntity<PagamentoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(pagamentoService.buscarPorId(id));
    }

    /** Registrar pagamento. */
    @PostMapping
    @Operation(summary = "Registrar pagamento")
    public ResponseEntity<PagamentoResponse> criar(@Valid @RequestBody PagamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamentoService.criar(request));
    }

    /** Cancelar pagamento. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar pagamento")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        pagamentoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
