package com.barbearia.saas.dto.fiscal;

import com.barbearia.saas.domain.enums.RegimeTributario;
import com.barbearia.saas.validation.Cnpj;
import com.barbearia.saas.validation.EmailReal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Configuração fiscal da barbearia para emissão de NFS-e. */
@Data
public class ConfigFiscalRequest {

    @Cnpj(optional = true)
    private String cnpj;

    @Size(max = 150)
    private String razaoSocial;

    @Size(max = 30)
    private String inscricaoMunicipal;

    @Pattern(regexp = "^$|^\\d{7}$", message = "Código do município IBGE deve ter 7 dígitos")
    private String codigoMunicipioIbge;

    @DecimalMin("0.00")
    private BigDecimal aliquotaIss;

    @Size(max = 10)
    private String codigoServicoPadrao;

    private RegimeTributario regimeTributario;

    private Boolean optanteSimples;

    private Boolean nfseHabilitada;

    /** Token Focus NFe da empresa (opcional se houver token global). */
    @Size(max = 255)
    private String nfseToken;

    @Size(max = 150)
    private String enderecoLogradouro;

    @Size(max = 20)
    private String enderecoNumero;

    @Size(max = 80)
    private String enderecoBairro;

    @Pattern(regexp = "^$|^\\d{8}$", message = "CEP deve ter 8 dígitos")
    private String enderecoCep;

    @Size(min = 2, max = 2)
    private String enderecoUf;

    @EmailReal(optional = true)
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String telefone;
}
