package com.barbearia.saas.dto.pagamento;

import com.barbearia.saas.domain.enums.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTO de entrada para registrar um pagamento. */
@Data
public class PagamentoRequest {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal valor;

    @NotNull
    private FormaPagamento formaPagamento;

    @NotNull
    private Long clienteId;

    @NotNull
    private Long servicoId;

    @NotNull
    private LocalDate dataPagamento;

    private Long agendamentoId;

    @Size(max = 255)
    private String descricao;
}
