package com.barbearia.saas.dto.ia;

import com.barbearia.saas.dto.agendamento.AgendamentoResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** DTO de saída com a resposta do chat de IA. */
@Data
@Builder
public class IaChatResponse {

    private String resposta;
    private String intencao;
    private IaContexto contexto;
    private AgendamentoResponse agendamento;

    @Builder.Default
    private List<ServicoItem> servicosSugeridos = new ArrayList<>();

    @Builder.Default
    private List<HorarioItem> horariosSugeridos = new ArrayList<>();

    @Builder.Default
    private List<String> acoesRapidas = new ArrayList<>();

    @Data
    @Builder
    public static class ServicoItem {
        private Long id;
        private String nome;
        private String descricao;
        private BigDecimal preco;
        private Integer duracaoMinutos;
        private String motivo;
    }

    @Data
    @Builder
    public static class HorarioItem {
        private String dataHora;
        private String label;
        private Long barbeiroId;
        private String barbeiroNome;
        private Long servicoId;
        private String servicoNome;
    }
}
