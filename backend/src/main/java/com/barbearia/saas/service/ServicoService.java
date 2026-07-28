package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Servico;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.ServicoRepository;
import com.barbearia.saas.dto.servico.ServicoRequest;
import com.barbearia.saas.dto.servico.ServicoResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** CRUD do catálogo de serviços da barbearia. */
@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final BarbeariaRepository barbeariaRepository;

    /** Lista os registros solicitados. */
    @Transactional(readOnly = true)
    public List<ServicoResponse> listar(boolean apenasAtivos) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        List<Servico> servicos = apenasAtivos
                ? servicoRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId)
                : servicoRepository.findByBarbeariaIdOrderByNomeAsc(barbeariaId);
        return servicos.stream().map(this::toResponse).toList();
    }

    /** Busca o registro pelo identificador informado. */
    @Transactional(readOnly = true)
    public ServicoResponse buscarPorId(Long id) {
        return toResponse(encontrarNaBarbearia(id));
    }

    /** Cria um novo registro. */
    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        String nome = request.getNome().trim();
        if (servicoRepository.existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrue(barbeariaId, nome)) {
            throw new NegocioException("Já existe serviço ativo com este nome");
        }

        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Servico servico = Servico.builder()
                .barbearia(barbearia)
                .nome(nome)
                .descricao(blankToNull(request.getDescricao()))
                .preco(request.getPreco())
                .duracaoMinutos(request.getDuracaoMinutos())
                .comissaoPercentual(defaultComissao(request.getComissaoPercentual()))
                .ativo(true)
                .build();

        return toResponse(servicoRepository.save(servico));
    }

    /** Atualiza o registro existente. */
    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        Servico servico = encontrarNaBarbearia(id);
        String nome = request.getNome().trim();
        if (servicoRepository.existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(
                SecurityUtils.getBarbeariaIdAtual(), nome, id)) {
            throw new NegocioException("Já existe serviço ativo com este nome");
        }

        servico.setNome(nome);
        servico.setDescricao(blankToNull(request.getDescricao()));
        servico.setPreco(request.getPreco());
        servico.setDuracaoMinutos(request.getDuracaoMinutos());
        servico.setComissaoPercentual(defaultComissao(request.getComissaoPercentual()));
        return toResponse(servicoRepository.save(servico));
    }

    /** Desativa o registro (soft delete). */
    @Transactional
    public void desativar(Long id) {
        Servico servico = encontrarNaBarbearia(id);
        servico.setAtivo(false);
        servicoRepository.save(servico);
    }

    private Servico encontrarNaBarbearia(Long id) {
        return servicoRepository.findByIdAndBarbeariaId(id, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));
    }

    private ServicoResponse toResponse(Servico servico) {
        return ServicoResponse.builder()
                .id(servico.getId())
                .nome(servico.getNome())
                .descricao(servico.getDescricao())
                .preco(servico.getPreco())
                .duracaoMinutos(servico.getDuracaoMinutos())
                .comissaoPercentual(servico.getComissaoPercentual())
                .ativo(servico.getAtivo())
                .build();
    }

    private BigDecimal defaultComissao(BigDecimal comissao) {
        return comissao != null ? comissao : BigDecimal.ZERO;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
