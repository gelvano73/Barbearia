package com.barbearia.saas.controller;

import com.barbearia.saas.domain.entity.Pagamento;
import com.barbearia.saas.domain.enums.StatusPagamento;
import com.barbearia.saas.domain.repository.PagamentoRepository;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.service.AssinaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Endpoints públicos auxiliares para o fluxo de pagamento (checkout simulado em desenvolvimento). */
@RestController
@RequestMapping("/api/public/pagamentos")
@RequiredArgsConstructor
@Tag(name = "Pagamentos públicos", description = "Endpoints públicos do fluxo de checkout")
public class PagamentoPublicoController {

    private final PagamentoRepository pagamentoRepository;
    private final AssinaturaService assinaturaService;

    /** Marca o pagamento/assinatura simulado (sem gateway configurado) como pago. Uso em desenvolvimento. */
    @GetMapping("/simulado/{referencia}")
    @Operation(summary = "Confirmar pagamento simulado (dev)")
    @Transactional
    public ResponseEntity<Map<String, Object>> confirmarSimulado(@PathVariable String referencia) {
        if (referencia != null && referencia.startsWith("assinatura-")) {
            assinaturaService.confirmarPagamentoAssinatura(referencia);
            return ResponseEntity.ok(Map.of(
                    "referencia", referencia,
                    "status", "ATIVA",
                    "mensagem", "Assinatura simulada ativada com sucesso"));
        }

        Long pagamentoId = Long.valueOf(referencia);
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado"));

        if (pagamento.getStatus() == StatusPagamento.PENDENTE) {
            pagamento.setStatus(StatusPagamento.PAGO);
            pagamento.setGatewayStatus("approved");
            pagamentoRepository.save(pagamento);
        }

        return ResponseEntity.ok(Map.of(
                "id", pagamento.getId(),
                "status", pagamento.getStatus(),
                "mensagem", "Pagamento simulado confirmado com sucesso"));
    }
}
