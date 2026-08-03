package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Unidade;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.UnidadeRepository;
import com.barbearia.saas.dto.unidade.UnidadeRequest;
import com.barbearia.saas.dto.unidade.UnidadeResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD de unidades físicas vinculadas à barbearia/franquia. */
@Service
@RequiredArgsConstructor
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final PlanoAcessoService planoAcessoService;

    /** === Consultas === */

    /** Lista os registros solicitados. */
    @Transactional(readOnly = true)
    public List<UnidadeResponse> listar(boolean apenasAtivos) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        List<Unidade> lista = apenasAtivos
                ? unidadeRepository.findByBarbeariaIdAndAtivoTrueOrderByPadraoDescNomeAsc(barbeariaId)
                : unidadeRepository.findByBarbeariaIdOrderByPadraoDescNomeAsc(barbeariaId);
        return lista.stream().map(this::toResponse).toList();
    }

    /** Busca o registro pelo identificador informado. */
    @Transactional(readOnly = true)
    public UnidadeResponse buscarPorId(Long id) {
        return toResponse(encontrarNaBarbearia(id));
    }

    /** === CRUD === */

    /** Cria um novo registro. */
    @Transactional
    public UnidadeResponse criar(UnidadeRequest request) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        planoAcessoService.exigirPodeCriarUnidade();
        String nome = request.getNome().trim();
        if (unidadeRepository.existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrue(barbeariaId, nome)) {
            throw new NegocioException("Já existe unidade ativa com este nome");
        }
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        boolean tornarPadrao = Boolean.TRUE.equals(request.getPadrao())
                || !unidadeRepository.existsByBarbeariaId(barbeariaId);
        if (tornarPadrao) {
            limparPadrao(barbeariaId);
        }

        Unidade unidade = Unidade.builder()
                .barbearia(barbearia)
                .nome(nome)
                .endereco(blankToNull(request.getEndereco()))
                .telefone(blankToNull(request.getTelefone()))
                .padrao(tornarPadrao)
                .ativo(true)
                .build();
        return toResponse(unidadeRepository.save(unidade));
    }

    /** Atualiza o registro existente. */
    @Transactional
    public UnidadeResponse atualizar(Long id, UnidadeRequest request) {
        Unidade unidade = encontrarNaBarbearia(id);
        String nome = request.getNome().trim();
        if (unidadeRepository.existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(
                SecurityUtils.getBarbeariaIdAtual(), nome, id)) {
            throw new NegocioException("Já existe unidade ativa com este nome");
        }

        unidade.setNome(nome);
        unidade.setEndereco(blankToNull(request.getEndereco()));
        unidade.setTelefone(blankToNull(request.getTelefone()));

        if (Boolean.TRUE.equals(request.getPadrao()) && !Boolean.TRUE.equals(unidade.getPadrao())) {
            limparPadrao(SecurityUtils.getBarbeariaIdAtual());
            unidade.setPadrao(true);
        }
        return toResponse(unidadeRepository.save(unidade));
    }

    /** Desativa o registro (soft delete). */
    @Transactional
    public void desativar(Long id) {
        Unidade unidade = encontrarNaBarbearia(id);
        if (Boolean.TRUE.equals(unidade.getPadrao())) {
            throw new NegocioException("Não é possível desativar a unidade padrão. Defina outra como padrão antes.");
        }
        unidade.setAtivo(false);
        unidadeRepository.save(unidade);
    }

    /** === Unidade padrão === */

    /** Cria a unidade padrão da barbearia. */
    @Transactional
    public Unidade criarPadrao(Barbearia barbearia) {
        return unidadeRepository.findFirstByBarbeariaIdAndPadraoTrueAndAtivoTrue(barbearia.getId())
                .orElseGet(() -> unidadeRepository.save(Unidade.builder()
                        .barbearia(barbearia)
                        .nome("Matriz")
                        .endereco(barbearia.getEndereco())
                        .telefone(barbearia.getTelefone())
                        .padrao(true)
                        .ativo(true)
                        .build()));
    }

    /** Obtém a unidade padrão ou cria se ainda não existir. */
    @Transactional
    public Unidade obterOuCriarPadrao(Long barbeariaId) {
        return unidadeRepository.findFirstByBarbeariaIdAndPadraoTrueAndAtivoTrue(barbeariaId)
                .orElseGet(() -> {
                    Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
                    return criarPadrao(barbearia);
                });
    }

    /** === Auxiliares === */

    private void limparPadrao(Long barbeariaId) {
        unidadeRepository.findFirstByBarbeariaIdAndPadraoTrueAndAtivoTrue(barbeariaId)
                .ifPresent(u -> {
                    u.setPadrao(false);
                    unidadeRepository.save(u);
                });
    }

    private Unidade encontrarNaBarbearia(Long id) {
        return unidadeRepository.findByIdAndBarbeariaId(id, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada"));
    }

    private UnidadeResponse toResponse(Unidade u) {
        return UnidadeResponse.builder()
                .id(u.getId())
                .nome(u.getNome())
                .endereco(u.getEndereco())
                .telefone(u.getTelefone())
                .padrao(u.getPadrao())
                .ativo(u.getAtivo())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
