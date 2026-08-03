package com.barbearia.saas.service;

import com.barbearia.saas.domain.enums.PlanoRecurso;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Empresa;
import com.barbearia.saas.domain.entity.Unidade;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.EmpresaRepository;
import com.barbearia.saas.domain.repository.UnidadeRepository;
import com.barbearia.saas.dto.franquia.*;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Visão consolidada de empresas/franquias e resumo por unidade. */
@Service
@RequiredArgsConstructor
public class FranquiaService {

    private final PlanoAcessoService planoAcessoService;

    private final EmpresaRepository empresaRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final UnidadeRepository unidadeRepository;

    /** Lista empresas. */
    @Transactional(readOnly = true)
    public List<EmpresaResponse> listarEmpresas() {
        return empresaRepository.findAllByOrderByNomeAsc().stream().map(this::toEmpresa).toList();
    }

    /** Cria empresa. */
    @Transactional
    public EmpresaResponse criarEmpresa(EmpresaRequest request) {
        planoAcessoService.exigirRecurso(PlanoRecurso.FRANQUIAS);
        if (request.getCnpj() != null && !request.getCnpj().isBlank()
                && empresaRepository.existsByCnpj(request.getCnpj().replaceAll("\\D", ""))) {
            throw new NegocioException("CNPJ já cadastrado");
        }
        Empresa empresa = empresaRepository.save(Empresa.builder()
                .nome(request.getNome().trim())
                .cnpj(normalizarCnpj(request.getCnpj()))
                .telefone(blankToNull(request.getTelefone()))
                .email(blankToNull(request.getEmail()))
                .ativo(true)
                .build());
        return toEmpresa(empresa);
    }

    /** Vincula a barbearia atual a uma empresa/franquia. */
    @Transactional
    public EmpresaResponse vincularBarbeariaAtual(Long empresaId) {
        planoAcessoService.exigirRecurso(PlanoRecurso.FRANQUIAS);
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada"));
        Barbearia barbearia = barbeariaRepository.findById(SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        barbearia.setEmpresa(empresa);
        barbeariaRepository.save(barbearia);
        return toEmpresa(empresa);
    }

    /** Retorna a visão consolidada da rede de franquias. */
    @Transactional(readOnly = true)
    public FranquiaVisaoResponse visaoRede() {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        Barbearia atual = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Empresa empresa = atual.getEmpresa();
        List<BarbeariaUnidadeResumo> unidadesRede = new java.util.ArrayList<>();

        if (empresa != null) {
            for (Barbearia b : barbeariaRepository.findByEmpresaIdOrderByNomeAsc(empresa.getId())) {
                List<Unidade> us = unidadeRepository.findByBarbeariaIdOrderByPadraoDescNomeAsc(b.getId());
                for (Unidade u : us) {
                    unidadesRede.add(BarbeariaUnidadeResumo.builder()
                            .barbeariaId(b.getId())
                            .barbeariaNome(b.getNome())
                            .unidadeId(u.getId())
                            .unidadeNome(u.getNome())
                            .padrao(u.getPadrao())
                            .ativo(u.getAtivo())
                            .build());
                }
                if (us.isEmpty()) {
                    unidadesRede.add(BarbeariaUnidadeResumo.builder()
                            .barbeariaId(b.getId())
                            .barbeariaNome(b.getNome())
                            .unidadeNome("(sem unidade)")
                            .padrao(false)
                            .ativo(b.getAtivo())
                            .build());
                }
            }
        } else {
            for (Unidade u : unidadeRepository.findByBarbeariaIdOrderByPadraoDescNomeAsc(barbeariaId)) {
                unidadesRede.add(BarbeariaUnidadeResumo.builder()
                        .barbeariaId(atual.getId())
                        .barbeariaNome(atual.getNome())
                        .unidadeId(u.getId())
                        .unidadeNome(u.getNome())
                        .padrao(u.getPadrao())
                        .ativo(u.getAtivo())
                        .build());
            }
        }

        return FranquiaVisaoResponse.builder()
                .empresa(empresa != null ? toEmpresa(empresa) : null)
                .barbeariaAtualId(atual.getId())
                .barbeariaAtualNome(atual.getNome())
                .multiempresa(empresa != null)
                .multiunidade(unidadesRede.size() > 1)
                .rede(unidadesRede)
                .build();
    }

    private EmpresaResponse toEmpresa(Empresa e) {
        long qtd = barbeariaRepository.findByEmpresaIdOrderByNomeAsc(e.getId()).size();
        return EmpresaResponse.builder()
                .id(e.getId())
                .nome(e.getNome())
                .cnpj(e.getCnpj())
                .telefone(e.getTelefone())
                .email(e.getEmail())
                .ativo(e.getAtivo())
                .quantidadeBarbearias(qtd)
                .build();
    }

    private String normalizarCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) return null;
        return cnpj.replaceAll("\\D", "");
    }

    private String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
