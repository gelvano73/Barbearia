package com.barbearia.saas.service.nfse;

import com.barbearia.saas.config.NfseProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente Focus NFe (API v2 NFS-e).
 * @see <a href="https://focusnfe.com.br/doc/#nfse_envio">Documentação Focus</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FocusNfseProvider implements NfseProvider {

    private final NfseProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public EmissaoResultado emitir(String token, EmissaoRequest request) {
        if (token == null || token.isBlank()) {
            return new EmissaoResultado(false, "erro", null, null, null, null,
                    "Token Focus NFe não configurado", null);
        }

        Map<String, Object> body = montarPayload(request);
        String url = properties.baseUrl() + "/v2/nfse?ref=" + request.referencia();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, authHeaders(token)),
                    String.class);

            return parseResposta(response.getBody(), response.getStatusCode().is2xxSuccessful());
        } catch (RestClientResponseException e) {
            log.warn("Focus NFS-e rejeitou emissão ref={}: {} {}", request.referencia(),
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            return parseResposta(e.getResponseBodyAsString(), false);
        } catch (Exception e) {
            log.error("Falha ao emitir NFS-e Focus ref={}", request.referencia(), e);
            return new EmissaoResultado(false, "erro", null, null, null, null,
                    e.getMessage(), null);
        }
    }

    @Override
    public EmissaoResultado consultar(String token, String referencia) {
        if (token == null || token.isBlank()) {
            return new EmissaoResultado(false, "erro", null, null, null, null,
                    "Token Focus NFe não configurado", null);
        }
        String url = properties.baseUrl() + "/v2/nfse/" + referencia;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);
            return parseResposta(response.getBody(), true);
        } catch (RestClientResponseException e) {
            return parseResposta(e.getResponseBodyAsString(), false);
        } catch (Exception e) {
            return new EmissaoResultado(false, "erro", null, null, null, null, e.getMessage(), null);
        }
    }

    private Map<String, Object> montarPayload(EmissaoRequest request) {
        Map<String, Object> prestador = new LinkedHashMap<>();
        prestador.put("cnpj", request.prestador().cnpj());
        prestador.put("inscricao_municipal", request.prestador().inscricaoMunicipal());
        prestador.put("codigo_municipio", request.prestador().codigoMunicipioIbge());

        Map<String, Object> tomador = new LinkedHashMap<>();
        tomador.put("cpf", request.tomador().cpf());
        tomador.put("razao_social", request.tomador().nome());
        if (request.tomador().email() != null) {
            tomador.put("email", request.tomador().email());
        }
        if (request.tomador().telefone() != null) {
            tomador.put("telefone", request.tomador().telefone().replaceAll("\\D", ""));
        }

        Map<String, Object> servico = new LinkedHashMap<>();
        servico.put("valor_servicos", request.servico().valorServicos());
        servico.put("iss_retido", request.servico().issRetido() ? "1" : "0");
        servico.put("item_lista_servico", request.servico().itemListaServico());
        servico.put("discriminacao", request.servico().discriminacao());
        servico.put("codigo_municipio", request.servico().codigoMunicipioIbge());
        if (request.servico().aliquotaIss() != null) {
            servico.put("aliquota", request.servico().aliquotaIss());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data_emissao", OffsetDateTime.now(ZoneOffset.of("-03:00")).toString());
        body.put("natureza_operacao", "1");
        body.put("optante_simples_nacional", request.servico().optanteSimples());
        body.put("incentivador_cultural", false);
        body.put("prestador", prestador);
        body.put("tomador", tomador);
        body.put("servico", servico);
        return body;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String basic = Base64.getEncoder().encodeToString((token + ":").getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basic);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private EmissaoResultado parseResposta(String json, boolean httpOk) {
        if (json == null || json.isBlank()) {
            return new EmissaoResultado(false, "erro", null, null, null, null,
                    "Resposta vazia do provedor", null);
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            String status = str(map.get("status"));
            String mensagem = str(map.get("mensagem")) ;
            if (mensagem == null) {
                mensagem = str(map.get("erros"));
            }
            boolean autorizado = "autorizado".equalsIgnoreCase(status)
                    || "autorizada".equalsIgnoreCase(status)
                    || "processando_autorizacao".equalsIgnoreCase(status);
            boolean sucesso = httpOk && (autorizado || "autorizado".equalsIgnoreCase(status)
                    || map.get("numero") != null);

            return new EmissaoResultado(
                    sucesso || "processando_autorizacao".equalsIgnoreCase(status),
                    status != null ? status : (httpOk ? "enviado" : "erro"),
                    str(map.get("numero")),
                    str(map.get("codigo_verificacao")),
                    str(map.get("url")),
                    str(map.get("caminho_xml_nota_fiscal")),
                    mensagem,
                    json
            );
        } catch (Exception e) {
            return new EmissaoResultado(false, "erro", null, null, null, null, json, json);
        }
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o);
        return s.isBlank() || "null".equals(s) ? null : s;
    }
}
