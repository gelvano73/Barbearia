package com.barbearia.saas.dto.servico;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de saída com dados do serviço. */
@Data
@Builder
public class ServicoResponse {
    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private Integer duracaoMinutos;
    private BigDecimal comissaoPercentual;
    private String codigoListaServico;
    private Boolean ativo;
}
