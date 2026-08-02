package com.barbearia.saas.service;

import com.barbearia.saas.config.NfseProperties;
import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.StatusNotaFiscal;
import com.barbearia.saas.domain.enums.StatusPagamento;
import com.barbearia.saas.domain.repository.NotaFiscalRepository;
import com.barbearia.saas.domain.repository.PagamentoRepository;
import com.barbearia.saas.dto.fiscal.NotaFiscalResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import com.barbearia.saas.service.nfse.NfseProvider;
import com.barbearia.saas.util.CnpjUtil;
import com.barbearia.saas.util.CpfUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Emissão e consulta de NFS-e de serviços prestados. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotaFiscalService {

    private final NotaFiscalRepository notaFiscalRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ConfigFiscalService configFiscalService;
    private final NfseProvider nfseProvider;
    private final NfseProperties nfseProperties;

    @Transactional(readOnly = true)
    public List<NotaFiscalResponse> listar() {
        return notaFiscalRepository
                .findByBarbeariaIdOrderByCriadoEmDesc(SecurityUtils.getBarbeariaIdAtual())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotaFiscalResponse buscar(Long id) {
        return toResponse(notaFiscalRepository
                .findByIdAndBarbeariaId(id, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nota fiscal não encontrada")));
    }

    @Transactional(readOnly = true)
    public NotaFiscalResponse porPagamento(Long pagamentoId) {
        return notaFiscalRepository.findByPagamentoId(pagamentoId)
                .filter(n -> n.getBarbearia().getId().equals(SecurityUtils.getBarbeariaIdAtual()))
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nota fiscal não encontrada para o pagamento"));
    }

    /** Emite NFS-e para pagamento confirmado (manual ou automático). */
    @Transactional
    public NotaFiscalResponse emitirParaPagamento(Long pagamentoId) {
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado"));

        if (pagamento.getStatus() != StatusPagamento.PAGO) {
            throw new NegocioException("Só é possível emitir NFS-e para pagamento confirmado (PAGO)");
        }

        NotaFiscal existente = notaFiscalRepository.findByPagamentoId(pagamentoId).orElse(null);
        if (existente != null && existente.getStatus() == StatusNotaFiscal.AUTORIZADA) {
            return toResponse(existente);
        }

        Barbearia barbearia = pagamento.getBarbearia();
        if (!Boolean.TRUE.equals(barbearia.getNfseHabilitada()) || !nfseProperties.isEnabled()) {
            throw new NegocioException("NFS-e não habilitada. Configure em Fiscal / NFS-e.");
        }

        String tomadorCpf = resolverCpfTomador(pagamento);
        if (!CpfUtil.isValidoParaNotaFiscal(tomadorCpf, nfseProperties.isRejeitarCpfExemplo())) {
            throw new NegocioException(
                    "CPF do tomador inválido ou de demonstração. Informe o CPF real cadastrado na Receita Federal.");
        }
        if (!CnpjUtil.isValido(barbearia.getCnpj())) {
            throw new NegocioException("CNPJ do prestador inválido (Receita Federal)");
        }

        String codigoServico = resolverCodigoServico(pagamento, barbearia);
        String discriminacao = resolverDiscriminacao(pagamento);
        String tomadorNome = pagamento.getCliente() != null ? pagamento.getCliente().getNome() : "Consumidor";

        NotaFiscal nota = existente != null ? existente : NotaFiscal.builder()
                .barbearia(barbearia)
                .pagamento(pagamento)
                .cliente(pagamento.getCliente())
                .referenciaExterna("pag-" + pagamento.getId())
                .build();

        nota.setTomadorCpf(CpfUtil.somenteDigitos(tomadorCpf));
        nota.setTomadorNome(tomadorNome);
        nota.setValorServicos(pagamento.getValor());
        nota.setAliquotaIss(barbearia.getAliquotaIss() != null ? barbearia.getAliquotaIss() : BigDecimal.ZERO);
        nota.setCodigoServico(codigoServico);
        nota.setDiscriminacao(discriminacao);
        nota.setStatus(StatusNotaFiscal.PROCESSANDO);
        nota.setMensagemErro(null);
        nota = notaFiscalRepository.save(nota);

        NfseProvider.EmissaoResultado resultado = nfseProvider.emitir(
                configFiscalService.resolverToken(barbearia),
                new NfseProvider.EmissaoRequest(
                        nota.getReferenciaExterna(),
                        new NfseProvider.Prestador(
                                CnpjUtil.somenteDigitos(barbearia.getCnpj()),
                                barbearia.getInscricaoMunicipal(),
                                barbearia.getCodigoMunicipioIbge(),
                                barbearia.getRazaoSocial() != null ? barbearia.getRazaoSocial() : barbearia.getNome()),
                        new NfseProvider.Tomador(
                                nota.getTomadorCpf(),
                                tomadorNome,
                                pagamento.getCliente() != null ? pagamento.getCliente().getEmail() : null,
                                pagamento.getCliente() != null ? pagamento.getCliente().getTelefone() : null),
                        new NfseProvider.ServicoNfse(
                                pagamento.getValor(),
                                nota.getAliquotaIss(),
                                codigoServico,
                                discriminacao,
                                barbearia.getCodigoMunicipioIbge(),
                                false,
                                Boolean.TRUE.equals(barbearia.getOptanteSimples()))
                ));

        aplicarResultado(nota, resultado);
        return toResponse(notaFiscalRepository.save(nota));
    }

    /** Tentativa automática (não lança se config incompleta — só registra log/erro). */
    @Transactional
    public void tentarEmitirAutomatico(Long pagamentoId) {
        if (!nfseProperties.isAutoEmitir() || !nfseProperties.isEnabled()) {
            return;
        }
        try {
            Pagamento pagamento = pagamentoRepository.findById(pagamentoId).orElse(null);
            if (pagamento == null || pagamento.getStatus() != StatusPagamento.PAGO) {
                return;
            }
            if (!Boolean.TRUE.equals(pagamento.getBarbearia().getNfseHabilitada())) {
                return;
            }
            if (notaFiscalRepository.findByPagamentoId(pagamentoId)
                    .filter(n -> n.getStatus() == StatusNotaFiscal.AUTORIZADA)
                    .isPresent()) {
                return;
            }
            emitirParaPagamento(pagamentoId);
        } catch (Exception e) {
            log.warn("Auto-emissão NFS-e pagamento {}: {}", pagamentoId, e.getMessage());
        }
    }

    @Transactional
    public NotaFiscalResponse consultarProvedor(Long id) {
        NotaFiscal nota = notaFiscalRepository
                .findByIdAndBarbeariaId(id, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nota fiscal não encontrada"));

        NfseProvider.EmissaoResultado resultado = nfseProvider.consultar(
                configFiscalService.resolverToken(nota.getBarbearia()),
                nota.getReferenciaExterna());
        aplicarResultado(nota, resultado);
        return toResponse(notaFiscalRepository.save(nota));
    }

    private void aplicarResultado(NotaFiscal nota, NfseProvider.EmissaoResultado resultado) {
        nota.setRespostaJson(resultado.rawJson());
        nota.setNumero(resultado.numero());
        nota.setCodigoVerificacao(resultado.codigoVerificacao());
        nota.setUrlPdf(resultado.urlPdf());
        nota.setUrlXml(resultado.urlXml());
        nota.setMensagemErro(resultado.mensagem());

        String st = resultado.status() != null ? resultado.status().toLowerCase() : "";
        if (st.contains("autoriz")) {
            nota.setStatus(StatusNotaFiscal.AUTORIZADA);
            nota.setEmitidoEm(LocalDateTime.now());
            nota.setMensagemErro(null);
        } else if (st.contains("processando") || st.contains("enviado")) {
            nota.setStatus(StatusNotaFiscal.PROCESSANDO);
        } else if (st.contains("cancel")) {
            nota.setStatus(StatusNotaFiscal.CANCELADA);
        } else if (resultado.sucesso()) {
            nota.setStatus(StatusNotaFiscal.PROCESSANDO);
        } else {
            nota.setStatus(StatusNotaFiscal.REJEITADA);
        }
    }

    private String resolverCpfTomador(Pagamento pagamento) {
        Cliente cliente = pagamento.getCliente();
        if (cliente == null) {
            throw new NegocioException("Pagamento sem cliente — CPF do tomador obrigatório para NFS-e");
        }
        if (cliente.getCpf() != null && !cliente.getCpf().isBlank()) {
            return cliente.getCpf();
        }
        if (cliente.getUsuario() != null && cliente.getUsuario().getCpf() != null) {
            return cliente.getUsuario().getCpf();
        }
        throw new NegocioException(
                "Cliente sem CPF. Cadastre o CPF real do tomador (Receita Federal) antes de emitir a nota.");
    }

    private String resolverCodigoServico(Pagamento pagamento, Barbearia barbearia) {
        Servico servico = pagamento.getServico();
        if (servico != null && servico.getCodigoListaServico() != null && !servico.getCodigoListaServico().isBlank()) {
            return servico.getCodigoListaServico().trim();
        }
        return barbearia.getCodigoServicoPadrao() != null ? barbearia.getCodigoServicoPadrao() : "6.02";
    }

    private String resolverDiscriminacao(Pagamento pagamento) {
        if (pagamento.getDescricao() != null && !pagamento.getDescricao().isBlank()) {
            return pagamento.getDescricao().trim();
        }
        if (pagamento.getServico() != null) {
            return "Serviço: " + pagamento.getServico().getNome();
        }
        return "Serviços de barbearia";
    }

    private NotaFiscalResponse toResponse(NotaFiscal n) {
        return NotaFiscalResponse.builder()
                .id(n.getId())
                .pagamentoId(n.getPagamento() != null ? n.getPagamento().getId() : null)
                .clienteId(n.getCliente() != null ? n.getCliente().getId() : null)
                .status(n.getStatus())
                .provedor(n.getProvedor())
                .referenciaExterna(n.getReferenciaExterna())
                .numero(n.getNumero())
                .codigoVerificacao(n.getCodigoVerificacao())
                .urlPdf(n.getUrlPdf())
                .urlXml(n.getUrlXml())
                .tomadorCpf(CpfUtil.formatar(n.getTomadorCpf()))
                .tomadorNome(n.getTomadorNome())
                .valorServicos(n.getValorServicos())
                .aliquotaIss(n.getAliquotaIss())
                .codigoServico(n.getCodigoServico())
                .discriminacao(n.getDiscriminacao())
                .mensagemErro(n.getMensagemErro())
                .emitidoEm(n.getEmitidoEm())
                .criadoEm(n.getCriadoEm())
                .build();
    }
}
