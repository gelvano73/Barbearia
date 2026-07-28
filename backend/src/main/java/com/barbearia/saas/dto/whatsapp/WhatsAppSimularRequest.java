package com.barbearia.saas.dto.whatsapp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para simular mensagem recebida no WhatsApp. */
@Data
public class WhatsAppSimularRequest {
    @NotBlank
    @Size(max = 20)
    private String telefone;

    @NotBlank
    @Size(max = 1000)
    private String mensagem;
}
