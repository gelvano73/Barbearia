package com.barbearia.saas.dto.barbeiro;

import com.barbearia.saas.validation.EmailReal;
import com.barbearia.saas.validation.SenhaForte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para criar conta de acesso do barbeiro. */
@Data
public class CriarContaBarbeiroRequest {

    @NotBlank
    @EmailReal
    @Size(max = 150)
    private String email;

    @NotBlank
    @SenhaForte
    private String senha;
}
