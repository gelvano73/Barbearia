package com.barbearia.saas.dto.ia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada de mensagem para o chat de atendimento por IA. */
@Data
public class IaChatRequest {

    @NotBlank
    @Size(max = 1000)
    private String mensagem;

    private IaContexto contexto;

    /** PORTAL (padrão) ou WHATSAPP */
    private String canal;
}
