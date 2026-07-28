package com.barbearia.saas.controller;

import com.barbearia.saas.domain.enums.StatusPedidoMarketplace;
import com.barbearia.saas.dto.marketplace.*;
import com.barbearia.saas.service.MarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Endpoints do marketplace interno de produtos entre unidades/franquias. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Venda de produtos online")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    /** Catálogo público de produtos. */
    @GetMapping("/api/public/marketplace/{barbeariaId}/produtos")
    @Operation(summary = "Catálogo público de produtos")
    public ResponseEntity<List<MarketplaceProdutoResponse>> catalogo(@PathVariable Long barbeariaId) {
        return ResponseEntity.ok(marketplaceService.catalogoPublico(barbeariaId));
    }

    /** Criar pedido online (público). */
    @PostMapping("/api/public/marketplace/{barbeariaId}/pedidos")
    @Operation(summary = "Criar pedido online (público)")
    public ResponseEntity<PedidoMarketplaceResponse> criarPedido(
            @PathVariable Long barbeariaId, @Valid @RequestBody MarketplacePedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(marketplaceService.criarPedido(barbeariaId, request));
    }

    /** Listar pedidos da barbearia. */
    @GetMapping("/api/marketplace/pedidos")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar pedidos da barbearia")
    public ResponseEntity<List<PedidoMarketplaceResponse>> pedidos() {
        return ResponseEntity.ok(marketplaceService.listarPedidos());
    }

    /** Retorna o status do serviço ou integração. */
    @PatchMapping("/api/marketplace/pedidos/{id}/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualizar status do pedido")
    public ResponseEntity<PedidoMarketplaceResponse> status(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        StatusPedidoMarketplace status = StatusPedidoMarketplace.valueOf(body.get("status"));
        return ResponseEntity.ok(marketplaceService.atualizarStatus(id, status));
    }
}
