package com.barbearia.saas.domain.entity;

import com.barbearia.saas.domain.enums.PlanoAssinatura;
import com.barbearia.saas.domain.enums.RegimeTributario;
import com.barbearia.saas.domain.enums.StatusAssinatura;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Entidade JPA do tenant (barbearia) no modelo multi-tenant do SaaS. */
@Entity
@Table(name = "barbearias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Barbearia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(unique = true, length = 18)
    private String cnpj;

    @Column(length = 20)
    private String telefone;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String endereco;

    /** Razão social do prestador (NFS-e). */
    @Column(name = "razao_social", length = 150)
    private String razaoSocial;

    @Column(name = "inscricao_municipal", length = 30)
    private String inscricaoMunicipal;

    @Column(name = "codigo_municipio_ibge", length = 7)
    private String codigoMunicipioIbge;

    @Column(name = "aliquota_iss", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal aliquotaIss = java.math.BigDecimal.ZERO;

    /** Item da lista de serviços LC 116/2003 (ex.: 6.02 cabeleireiros). */
    @Column(name = "codigo_servico_padrao", length = 10)
    @Builder.Default
    private String codigoServicoPadrao = "6.02";

    @Enumerated(EnumType.STRING)
    @Column(name = "regime_tributario", length = 30)
    @Builder.Default
    private RegimeTributario regimeTributario = RegimeTributario.SIMPLES_NACIONAL;

    @Column(name = "optante_simples")
    @Builder.Default
    private Boolean optanteSimples = true;

    @Column(name = "nfse_habilitada")
    @Builder.Default
    private Boolean nfseHabilitada = false;

    @Column(name = "nfse_token", length = 255)
    private String nfseToken;

    @Column(name = "endereco_logradouro", length = 150)
    private String enderecoLogradouro;

    @Column(name = "endereco_numero", length = 20)
    private String enderecoNumero;

    @Column(name = "endereco_bairro", length = 80)
    private String enderecoBairro;

    @Column(name = "endereco_cep", length = 8)
    private String enderecoCep;

    @Column(name = "endereco_uf", length = 2)
    private String enderecoUf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PlanoAssinatura plano = PlanoAssinatura.TRIAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "assinatura_status", nullable = false, length = 30)
    @Builder.Default
    private StatusAssinatura assinaturaStatus = StatusAssinatura.ATIVA;

    @Column(name = "assinatura_vence_em")
    private LocalDateTime assinaturaVenceEm;

    @Column(name = "mp_customer_id", length = 80)
    private String mpCustomerId;

    @Column(name = "mp_subscription_id", length = 80)
    private String mpSubscriptionId;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
