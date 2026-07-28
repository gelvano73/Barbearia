package com.barbearia.saas.dto.marketplace;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** DTO de entrada para criar pedido no marketplace. */
@Data
public class MarketplacePedidoRequest {
    @NotBlank
    @Size(max = 150)
    private String clienteNome;

    @NotBlank
    @Size(max = 20)
    private String clienteTelefone;

    @Email
    @Size(max = 150)
    private String clienteEmail;

    @Size(max = 255)
    private String enderecoEntrega;

    @Size(max = 500)
    private String observacoes;

    /** Aceite da Política de Privacidade no checkout. */
    private boolean aceitePrivacidade;

    @AssertTrue(message = "É necessário aceitar a Política de Privacidade")
    public boolean isAceitePrivacidadeOk() {
        return aceitePrivacidade;
    }

    @NotEmpty
    @Valid
    private List<MarketplaceItemRequest> itens;
}
