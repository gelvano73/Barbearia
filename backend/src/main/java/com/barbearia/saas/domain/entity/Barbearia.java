package com.barbearia.saas.domain.entity;

import com.barbearia.saas.domain.enums.PlanoAssinatura;
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
