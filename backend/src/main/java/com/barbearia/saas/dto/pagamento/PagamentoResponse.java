package com.barbearia.saas.dto.pagamento;

import com.barbearia.saas.domain.enums.FormaPagamento;
import com.barbearia.saas.domain.enums.StatusPagamento;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** DTO de saída com dados de um pagamento. */
@Data
@Builder
public class PagamentoResponse {
    private Long id;
    private Long caixaId;
    private Long clienteId;
    private String clienteNome;
    private Long servicoId;
    private String servicoNome;
    private Long agendamentoId;
    private BigDecimal valor;
    private FormaPagamento formaPagamento;
    private StatusPagamento status;
    private LocalDate dataPagamento;
    private String descricao;
    private LocalDateTime criadoEm;
}
