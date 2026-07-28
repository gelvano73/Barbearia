package com.barbearia.saas.dto.marketplace;

import com.barbearia.saas.domain.enums.StatusPedidoMarketplace;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTO de saída de um pedido do marketplace. */
@Data
@Builder
public class PedidoMarketplaceResponse {
    private Long id;
    private String clienteNome;
    private String clienteTelefone;
    private String clienteEmail;
    private String enderecoEntrega;
    private StatusPedidoMarketplace status;
    private BigDecimal total;
    private String observacoes;
    private LocalDateTime criadoEm;
    private List<PedidoItemResponse> itens;
}
