package com.barbearia.saas.dto.ia;

import lombok.Data;

/** DTO com contexto da barbearia enviado ao provedor de IA. */
@Data
public class IaContexto {
    private Long barbeiroId;
    private Long servicoId;
    private String dataHora;
    private Boolean aguardandoConfirmacao;
}
