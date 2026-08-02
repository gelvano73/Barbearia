package com.barbearia.saas.service.nfse;

import java.math.BigDecimal;
import java.util.Map;

/** Contrato de provedor de NFS-e (Focus NFe, etc.). */
public interface NfseProvider {

    record Prestador(
            String cnpj,
            String inscricaoMunicipal,
            String codigoMunicipioIbge,
            String razaoSocial
    ) {
    }

    record Tomador(
            String cpf,
            String nome,
            String email,
            String telefone
    ) {
    }

    record ServicoNfse(
            BigDecimal valorServicos,
            BigDecimal aliquotaIss,
            String itemListaServico,
            String discriminacao,
            String codigoMunicipioIbge,
            boolean issRetido,
            boolean optanteSimples
    ) {
    }

    record EmissaoRequest(
            String referencia,
            Prestador prestador,
            Tomador tomador,
            ServicoNfse servico
    ) {
    }

    record EmissaoResultado(
            boolean sucesso,
            String status,
            String numero,
            String codigoVerificacao,
            String urlPdf,
            String urlXml,
            String mensagem,
            String rawJson
    ) {
    }

    EmissaoResultado emitir(String token, EmissaoRequest request);

    EmissaoResultado consultar(String token, String referencia);
}
