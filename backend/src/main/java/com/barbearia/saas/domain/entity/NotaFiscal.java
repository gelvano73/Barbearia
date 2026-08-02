package com.barbearia.saas.domain.entity;

import com.barbearia.saas.domain.enums.StatusNotaFiscal;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** NFS-e emitida para um pagamento de serviço. */
@Entity
@Table(name = "notas_fiscais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false)
    private Barbearia barbearia;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pagamento_id", nullable = false, unique = true)
    private Pagamento pagamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusNotaFiscal status = StatusNotaFiscal.PENDENTE;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String provedor = "FOCUS_NFE";

    @Column(name = "referencia_externa", nullable = false, length = 80)
    private String referenciaExterna;

    @Column(length = 40)
    private String numero;

    @Column(name = "codigo_verificacao", length = 80)
    private String codigoVerificacao;

    @Column(name = "url_pdf", length = 500)
    private String urlPdf;

    @Column(name = "url_xml", length = 500)
    private String urlXml;

    @Column(name = "tomador_cpf", nullable = false, length = 11)
    private String tomadorCpf;

    @Column(name = "tomador_nome", nullable = false, length = 150)
    private String tomadorNome;

    @Column(name = "valor_servicos", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorServicos;

    @Column(name = "aliquota_iss", precision = 5, scale = 2)
    private BigDecimal aliquotaIss;

    @Column(name = "codigo_servico", length = 10)
    private String codigoServico;

    @Column(length = 1000)
    private String discriminacao;

    @Column(name = "mensagem_erro", length = 1000)
    private String mensagemErro;

    @Column(name = "resposta_json", columnDefinition = "TEXT")
    private String respostaJson;

    @Column(name = "emitido_em")
    private LocalDateTime emitidoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
