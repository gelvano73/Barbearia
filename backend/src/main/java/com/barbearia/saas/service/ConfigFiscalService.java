package com.barbearia.saas.service;

import com.barbearia.saas.domain.enums.PlanoRecurso;

import com.barbearia.saas.config.NfseProperties;
import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.enums.RegimeTributario;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.dto.fiscal.ConfigFiscalRequest;
import com.barbearia.saas.dto.fiscal.ConfigFiscalResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import com.barbearia.saas.util.CnpjUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Configuração fiscal da barbearia (CNPJ, IM, município, regime, token Focus)
 * e avaliação de prontidão para emitir NFS-e.
 */
@Service
@RequiredArgsConstructor
public class ConfigFiscalService {

    private final PlanoAcessoService planoAcessoService;

    private final BarbeariaRepository barbeariaRepository;
    private final NfseProperties nfseProperties;
    private final EmailDominioService emailDominioService;

    /** === Configuração === */

    @Transactional(readOnly = true)
    public ConfigFiscalResponse obter() {
        return toResponse(barbeariaAtual());
    }

    @Transactional
    public ConfigFiscalResponse salvar(ConfigFiscalRequest request) {
        planoAcessoService.exigirRecurso(PlanoRecurso.NFSE);
        Barbearia b = barbeariaAtual();

        if (request.getCnpj() != null && !request.getCnpj().isBlank()) {
            String cnpj = CnpjUtil.somenteDigitos(request.getCnpj());
            if (!CnpjUtil.isValido(cnpj)) {
                throw new NegocioException("CNPJ inválido segundo dígitos verificadores da Receita Federal");
            }
            b.setCnpj(cnpj);
        }
        if (request.getRazaoSocial() != null) {
            b.setRazaoSocial(blankToNull(request.getRazaoSocial()));
        }
        if (request.getInscricaoMunicipal() != null) {
            b.setInscricaoMunicipal(blankToNull(request.getInscricaoMunicipal()));
        }
        if (request.getCodigoMunicipioIbge() != null) {
            b.setCodigoMunicipioIbge(blankToNull(request.getCodigoMunicipioIbge()));
        }
        if (request.getAliquotaIss() != null) {
            b.setAliquotaIss(request.getAliquotaIss());
        }
        if (request.getCodigoServicoPadrao() != null) {
            b.setCodigoServicoPadrao(blankToNull(request.getCodigoServicoPadrao()));
        }
        if (request.getRegimeTributario() != null) {
            b.setRegimeTributario(request.getRegimeTributario());
        }
        if (request.getOptanteSimples() != null) {
            b.setOptanteSimples(request.getOptanteSimples());
        }
        if (request.getNfseHabilitada() != null) {
            b.setNfseHabilitada(request.getNfseHabilitada());
        }
        if (request.getNfseToken() != null && !request.getNfseToken().isBlank()) {
            b.setNfseToken(request.getNfseToken().trim());
        }
        if (request.getEnderecoLogradouro() != null) {
            b.setEnderecoLogradouro(blankToNull(request.getEnderecoLogradouro()));
        }
        if (request.getEnderecoNumero() != null) {
            b.setEnderecoNumero(blankToNull(request.getEnderecoNumero()));
        }
        if (request.getEnderecoBairro() != null) {
            b.setEnderecoBairro(blankToNull(request.getEnderecoBairro()));
        }
        if (request.getEnderecoCep() != null) {
            String cep = request.getEnderecoCep().replaceAll("\\D", "");
            b.setEnderecoCep(cep.isBlank() ? null : cep);
        }
        if (request.getEnderecoUf() != null) {
            b.setEnderecoUf(blankToNull(request.getEnderecoUf()) != null
                    ? request.getEnderecoUf().trim().toUpperCase() : null);
        }
        if (request.getEmail() != null) {
            if (request.getEmail().isBlank()) {
                b.setEmail(null);
            } else {
                emailDominioService.validarOuFalhar(request.getEmail());
                b.setEmail(com.barbearia.saas.util.EmailUtil.normalizar(request.getEmail()));
            }
        }
        if (request.getTelefone() != null) {
            b.setTelefone(blankToNull(request.getTelefone()));
        }

        if (Boolean.TRUE.equals(b.getNfseHabilitada())) {
            validarMinimoParaHabilitar(b);
        }

        return toResponse(barbeariaRepository.save(b));
    }

    /** === Validação e token === */

    private void validarMinimoParaHabilitar(Barbearia b) {
        if (b.getCnpj() == null || !CnpjUtil.isValido(b.getCnpj())) {
            throw new NegocioException("Informe um CNPJ válido (Receita Federal) para habilitar a NFS-e");
        }
        if (blank(b.getInscricaoMunicipal())) {
            throw new NegocioException("Inscrição municipal é obrigatória para NFS-e");
        }
        if (blank(b.getCodigoMunicipioIbge()) || b.getCodigoMunicipioIbge().length() != 7) {
            throw new NegocioException("Código do município IBGE (7 dígitos) é obrigatório");
        }
        if (blank(b.getCodigoServicoPadrao())) {
            throw new NegocioException("Código do serviço (lista LC 116) é obrigatório — ex.: 6.02");
        }
        String token = resolverToken(b);
        if (blank(token) && nfseProperties.isEnabled()) {
            throw new NegocioException("Configure o token Focus NFe (da barbearia ou global)");
        }
    }

    public String resolverToken(Barbearia b) {
        if (b.getNfseToken() != null && !b.getNfseToken().isBlank()) {
            return b.getNfseToken().trim();
        }
        return nfseProperties.getToken() != null ? nfseProperties.getToken().trim() : "";
    }

    /** === Auxiliares === */

    private Barbearia barbeariaAtual() {
        return barbeariaRepository.findById(SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
    }

    private ConfigFiscalResponse toResponse(Barbearia b) {
        String msg = avaliarProntidao(b);
        return ConfigFiscalResponse.builder()
                .cnpj(b.getCnpj())
                .razaoSocial(b.getRazaoSocial() != null ? b.getRazaoSocial() : b.getNome())
                .inscricaoMunicipal(b.getInscricaoMunicipal())
                .codigoMunicipioIbge(b.getCodigoMunicipioIbge())
                .aliquotaIss(b.getAliquotaIss() != null ? b.getAliquotaIss() : BigDecimal.ZERO)
                .codigoServicoPadrao(b.getCodigoServicoPadrao() != null ? b.getCodigoServicoPadrao() : "6.02")
                .regimeTributario(b.getRegimeTributario() != null ? b.getRegimeTributario() : RegimeTributario.SIMPLES_NACIONAL)
                .optanteSimples(b.getOptanteSimples())
                .nfseHabilitada(Boolean.TRUE.equals(b.getNfseHabilitada()))
                .possuiToken(!blank(resolverToken(b)))
                .enderecoLogradouro(b.getEnderecoLogradouro())
                .enderecoNumero(b.getEnderecoNumero())
                .enderecoBairro(b.getEnderecoBairro())
                .enderecoCep(b.getEnderecoCep())
                .enderecoUf(b.getEnderecoUf())
                .email(b.getEmail())
                .telefone(b.getTelefone())
                .ambiente(nfseProperties.getAmbiente())
                .integracaoGlobalAtiva(nfseProperties.isEnabled())
                .prontoParaEmitir(msg == null)
                .mensagemProntidao(msg != null ? msg : "Configuração fiscal pronta para emitir NFS-e")
                .build();
    }

    private String avaliarProntidao(Barbearia b) {
        if (!nfseProperties.isEnabled()) {
            return "Integração NFS-e desligada no servidor (NFSE_ENABLED=true)";
        }
        if (!Boolean.TRUE.equals(b.getNfseHabilitada())) {
            return "Habilite a emissão de NFS-e nesta barbearia";
        }
        if (b.getCnpj() == null || !CnpjUtil.isValido(b.getCnpj())) {
            return "CNPJ do prestador inválido ou ausente";
        }
        if (blank(b.getInscricaoMunicipal())) {
            return "Falta inscrição municipal";
        }
        if (blank(b.getCodigoMunicipioIbge())) {
            return "Falta código IBGE do município";
        }
        if (blank(resolverToken(b))) {
            return "Falta token Focus NFe";
        }
        return null;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
