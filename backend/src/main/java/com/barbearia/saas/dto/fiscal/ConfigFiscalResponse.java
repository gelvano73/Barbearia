package com.barbearia.saas.dto.fiscal;

import com.barbearia.saas.domain.enums.RegimeTributario;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ConfigFiscalResponse {
    private String cnpj;
    private String razaoSocial;
    private String inscricaoMunicipal;
    private String codigoMunicipioIbge;
    private BigDecimal aliquotaIss;
    private String codigoServicoPadrao;
    private RegimeTributario regimeTributario;
    private Boolean optanteSimples;
    private Boolean nfseHabilitada;
    private boolean possuiToken;
    private String enderecoLogradouro;
    private String enderecoNumero;
    private String enderecoBairro;
    private String enderecoCep;
    private String enderecoUf;
    private String email;
    private String telefone;
    private String ambiente;
    private boolean integracaoGlobalAtiva;
    private boolean prontoParaEmitir;
    private String mensagemProntidao;
}
