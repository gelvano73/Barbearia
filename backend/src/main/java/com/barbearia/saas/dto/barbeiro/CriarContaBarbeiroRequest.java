package com.barbearia.saas.dto.barbeiro;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para criar conta de acesso do barbeiro. */
@Data
public class CriarContaBarbeiroRequest {

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String senha;
}
