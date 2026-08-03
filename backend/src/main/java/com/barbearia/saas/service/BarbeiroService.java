package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Barbeiro;
import com.barbearia.saas.domain.entity.BarbeiroMeta;
import com.barbearia.saas.domain.entity.Usuario;
import com.barbearia.saas.domain.enums.Role;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.BarbeiroMetaRepository;
import com.barbearia.saas.domain.repository.BarbeiroRepository;
import com.barbearia.saas.domain.repository.UsuarioRepository;
import com.barbearia.saas.dto.barbeiro.BarbeiroRequest;
import com.barbearia.saas.dto.barbeiro.BarbeiroResponse;
import com.barbearia.saas.dto.barbeiro.CriarContaBarbeiroRequest;
import com.barbearia.saas.dto.barbeiro.MetaRequest;
import com.barbearia.saas.dto.portalbarbeiro.MetaProgressoResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import com.barbearia.saas.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Cadastro e manutenção de barbeiros: CRUD, foto, conta de acesso (role BARBEIRO)
 * e metas mensais de atendimentos/comissão.
 */
@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private final BarbeiroRepository barbeiroRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final BarbeiroMetaRepository metaRepository;
    private final PasswordEncoder passwordEncoder;
    private final FotoStorageService fotoStorageService;
    private final EmailDominioService emailDominioService;
    private final PlanoAcessoService planoAcessoService;

    /** === Consultas === */

    /** Lista barbeiros da barbearia atual (ativos ou todos). */
    @Transactional(readOnly = true)
    public List<BarbeiroResponse> listar(boolean apenasAtivos) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        List<Barbeiro> barbeiros = apenasAtivos
                ? barbeiroRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId)
                : barbeiroRepository.findByBarbeariaIdOrderByNomeAsc(barbeariaId);
        return barbeiros.stream().map(this::toResponse).toList();
    }

    /** Busca barbeiro pelo id, restrito à barbearia do usuário autenticado. */
    @Transactional(readOnly = true)
    public BarbeiroResponse buscarPorId(Long id) {
        return toResponse(encontrarNaBarbearia(id));
    }

    /** === CRUD === */

    /** Cadastra um novo barbeiro na barbearia atual. */
    @Transactional
    public BarbeiroResponse criar(BarbeiroRequest request) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        planoAcessoService.exigirPodeCriarBarbeiro();
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Barbeiro barbeiro = Barbeiro.builder()
                .barbearia(barbearia)
                .nome(request.getNome().trim())
                .telefone(blankToNull(request.getTelefone()))
                .especialidade(blankToNull(request.getEspecialidade()))
                .ativo(true)
                .build();

        return toResponse(barbeiroRepository.save(barbeiro));
    }

    /** Atualiza dados cadastrais do barbeiro. */
    @Transactional
    public BarbeiroResponse atualizar(Long id, BarbeiroRequest request) {
        Barbeiro barbeiro = encontrarNaBarbearia(id);
        barbeiro.setNome(request.getNome().trim());
        barbeiro.setTelefone(blankToNull(request.getTelefone()));
        barbeiro.setEspecialidade(blankToNull(request.getEspecialidade()));
        return toResponse(barbeiroRepository.save(barbeiro));
    }

    /** Faz upload e associa a foto de perfil do barbeiro. */
    @Transactional
    public BarbeiroResponse uploadFoto(Long id, MultipartFile arquivo) {
        Barbeiro barbeiro = encontrarNaBarbearia(id);
        String url = fotoStorageService.salvar(arquivo, "barbeiros", "barbeiro-" + barbeiro.getId());
        barbeiro.setFotoUrl(url);
        return toResponse(barbeiroRepository.save(barbeiro));
    }

    /** Desativa o barbeiro (soft delete). */
    @Transactional
    public void desativar(Long id) {
        Barbeiro barbeiro = encontrarNaBarbearia(id);
        barbeiro.setAtivo(false);
        barbeiroRepository.save(barbeiro);
    }

    /** === Conta e metas === */

    /** Cria usuário de acesso (role BARBEIRO) vinculado ao cadastro. */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public BarbeiroResponse criarConta(Long id, CriarContaBarbeiroRequest request) {
        Barbeiro barbeiro = encontrarNaBarbearia(id);
        if (barbeiro.getUsuario() != null) {
            throw new NegocioException("Barbeiro já possui conta de acesso");
        }
        emailDominioService.validarOuFalhar(request.getEmail());
        String email = EmailUtil.normalizar(request.getEmail());
        if (usuarioRepository.existsByEmail(email)) {
            throw new NegocioException("Email já cadastrado");
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .barbearia(barbeiro.getBarbearia())
                .nome(barbeiro.getNome())
                .email(email)
                .senhaHash(passwordEncoder.encode(request.getSenha()))
                .role(Role.BARBEIRO)
                .ativo(true)
                .build());

        barbeiro.setUsuario(usuario);
        return toResponse(barbeiroRepository.save(barbeiro));
    }

    /** Define ou atualiza a meta mensal de atendimentos/comissão. */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public MetaProgressoResponse definirMeta(Long id, MetaRequest request) {
        Barbeiro barbeiro = encontrarNaBarbearia(id);
        BarbeiroMeta meta = metaRepository
                .findByBarbeiroIdAndAnoAndMes(barbeiro.getId(), request.getAno(), request.getMes())
                .orElse(BarbeiroMeta.builder()
                        .barbeiro(barbeiro)
                        .ano(request.getAno())
                        .mes(request.getMes())
                        .build());
        meta.setMetaAtendimentos(request.getMetaAtendimentos());
        meta.setMetaComissao(request.getMetaComissao());
        metaRepository.save(meta);

        return MetaProgressoResponse.builder()
                .ano(meta.getAno())
                .mes(meta.getMes())
                .metaAtendimentos(meta.getMetaAtendimentos())
                .metaComissao(meta.getMetaComissao())
                .atendimentosRealizados(0L)
                .comissaoRealizada(java.math.BigDecimal.ZERO)
                .percentualAtendimentos(0.0)
                .percentualComissao(0.0)
                .build();
    }

    /** === Auxiliares === */

    private Barbeiro encontrarNaBarbearia(Long id) {
        return barbeiroRepository.findByIdAndBarbeariaId(id, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbeiro não encontrado"));
    }

    private BarbeiroResponse toResponse(Barbeiro barbeiro) {
        return BarbeiroResponse.builder()
                .id(barbeiro.getId())
                .nome(barbeiro.getNome())
                .telefone(barbeiro.getTelefone())
                .especialidade(barbeiro.getEspecialidade())
                .fotoUrl(barbeiro.getFotoUrl())
                .ativo(barbeiro.getAtivo())
                .usuarioId(barbeiro.getUsuario() != null ? barbeiro.getUsuario().getId() : null)
                .criadoEm(barbeiro.getCriadoEm())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
