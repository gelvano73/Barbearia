package com.barbearia.saas.dto.recepcao;

import com.barbearia.saas.domain.enums.FormaPagamento;
import com.barbearia.saas.domain.enums.StatusCaixa;
import com.barbearia.saas.domain.enums.TipoMovimentoCaixa;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTO de saída com dados do caixa. */
@Data
@Builder
public class CaixaResponse {
    private Long id;
    private Long usuarioId;
    private String usuarioNome;
    private LocalDateTime abertoEm;
    private LocalDateTime fechadoEm;
    private BigDecimal valorAbertura;
    private BigDecimal valorInformadoFechamento;
    private BigDecimal totalEntradas;
    private BigDecimal totalSaidas;
    private BigDecimal totalSangrias;
    private BigDecimal totalSuprimentos;
    private BigDecimal saldoCalculado;
    private StatusCaixa status;
    private String observacoes;
    private List<MovimentoItem> movimentos;

    @Data
    @Builder
    public static class MovimentoItem {
        private Long id;
        private TipoMovimentoCaixa tipo;
        private FormaPagamento formaPagamento;
        private BigDecimal valor;
        private String descricao;
        private LocalDateTime criadoEm;
    }
}
