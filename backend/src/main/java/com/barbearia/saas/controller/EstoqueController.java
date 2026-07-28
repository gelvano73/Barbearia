package com.barbearia.saas.controller;

import com.barbearia.saas.dto.estoque.EstoqueMovimentoRequest;
import com.barbearia.saas.dto.estoque.EstoqueMovimentoResponse;
import com.barbearia.saas.dto.estoque.ProdutoRequest;
import com.barbearia.saas.dto.estoque.ProdutoResponse;
import com.barbearia.saas.service.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints de produtos e movimentos de estoque. */
@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Estoque", description = "Controle de estoque: produtos, entrada, saída e inventário")
public class EstoqueController {

    private final EstoqueService estoqueService;

    /** Listar produtos (seed automático na primeira vez). */
    @GetMapping("/produtos")
    @Operation(summary = "Listar produtos (seed automático na primeira vez)")
    public ResponseEntity<List<ProdutoResponse>> listarProdutos(
            @RequestParam(defaultValue = "true") boolean apenasAtivos) {
        return ResponseEntity.ok(estoqueService.listarOuSeed(apenasAtivos));
    }

    /** Cadastrar produto. */
    @PostMapping("/produtos")
    @Operation(summary = "Cadastrar produto")
    public ResponseEntity<ProdutoResponse> criarProduto(@Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estoqueService.criarProduto(request));
    }

    /** Atualizar produto. */
    @PutMapping("/produtos/{id}")
    @Operation(summary = "Atualizar produto")
    public ResponseEntity<ProdutoResponse> atualizarProduto(
            @PathVariable Long id, @Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.ok(estoqueService.atualizarProduto(id, request));
    }

    /** Desativar produto. */
    @DeleteMapping("/produtos/{id}")
    @Operation(summary = "Desativar produto")
    public ResponseEntity<Void> desativarProduto(@PathVariable Long id) {
        estoqueService.desativarProduto(id);
        return ResponseEntity.noContent().build();
    }

    /** Histórico de movimentos. */
    @GetMapping("/movimentos")
    @Operation(summary = "Histórico de movimentos")
    public ResponseEntity<List<EstoqueMovimentoResponse>> movimentos(
            @RequestParam(required = false) Long produtoId) {
        return ResponseEntity.ok(estoqueService.listarMovimentos(produtoId));
    }

    /** Registra um movimento no caixa. */
    @PostMapping("/movimentos")
    @Operation(summary = "Registrar entrada, saída ou inventário")
    public ResponseEntity<EstoqueMovimentoResponse> movimentar(
            @Valid @RequestBody EstoqueMovimentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estoqueService.movimentar(request));
    }
}
