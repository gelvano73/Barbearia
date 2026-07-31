package com.barbearia.saas.domain.entity;

import com.barbearia.saas.domain.enums.CanalNotificacao;
import com.barbearia.saas.domain.enums.StatusNotificacao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Entidade JPA de auditoria/registro de notificações enviadas (email, WhatsApp, etc.). */
@Entity
@Table(name = "notificacao_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacaoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "barbearia_id", nullable = false)
    private Long barbeariaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacao canal;

    @Column(length = 150)
    private String destino;

    @Column(length = 200)
    private String assunto;

    @Column(columnDefinition = "TEXT")
    private String corpo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusNotificacao status = StatusNotificacao.ENVIADO;

    @Column(length = 120)
    private String referencia;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
