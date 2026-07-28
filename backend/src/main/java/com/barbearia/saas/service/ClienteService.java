package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Cliente;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.ClienteRepository;
import com.barbearia.saas.dto.cliente.ClienteRequest;
import com.barbearia.saas.dto.cliente.ClienteResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** CRUD de clientes da barbearia, incluindo upload de foto. */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final FotoStorageService fotoStorageService;

    /** Lista os registros solicitados. */
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(boolean apenasAtivos) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        List<Cliente> clientes = apenasAtivos
                ? clienteRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId)
                : clienteRepository.findByBarbeariaIdOrderByNomeAsc(barbeariaId);
        return clientes.stream().map(this::toResponse).toList();
    }

    /** Busca o registro pelo identificador informado. */
    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return toResponse(encontrarNaBarbearia(id));
    }

    /** Cria um novo registro. */
    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        if (clienteRepository.existsByBarbeariaIdAndTelefoneAndAtivoTrue(barbeariaId, request.getTelefone())) {
            throw new NegocioException("Já existe cliente ativo com este telefone");
        }

        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Cliente cliente = Cliente.builder()
                .barbearia(barbearia)
                .nome(request.getNome().trim())
                .telefone(request.getTelefone().trim())
                .email(blankToNull(request.getEmail()))
                .observacoes(blankToNull(request.getObservacoes()))
                .ativo(true)
                .build();

        return toResponse(clienteRepository.save(cliente));
    }

    /** Atualiza o registro existente. */
    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = encontrarNaBarbearia(id);
        cliente.setNome(request.getNome().trim());
        cliente.setTelefone(request.getTelefone().trim());
        cliente.setEmail(blankToNull(request.getEmail()));
        cliente.setObservacoes(blankToNull(request.getObservacoes()));
        return toResponse(clienteRepository.save(cliente));
    }

    /** Faz upload e associa a foto ao registro. */
    @Transactional
    public ClienteResponse uploadFoto(Long id, MultipartFile arquivo) {
        Cliente cliente = encontrarNaBarbearia(id);
        String url = fotoStorageService.salvar(arquivo, "clientes", "cliente-" + cliente.getId());
        cliente.setFotoUrl(url);
        return toResponse(clienteRepository.save(cliente));
    }

    /** Desativa o registro (soft delete). */
    @Transactional
    public void desativar(Long id) {
        Cliente cliente = encontrarNaBarbearia(id);
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    private Cliente encontrarNaBarbearia(Long id) {
        return clienteRepository.findByIdAndBarbeariaId(id, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
    }

    private ClienteResponse toResponse(Cliente cliente) {
        return ClienteResponse.builder()
                .id(cliente.getId())
                .nome(cliente.getNome())
                .telefone(cliente.getTelefone())
                .email(cliente.getEmail())
                .observacoes(cliente.getObservacoes())
                .fotoUrl(cliente.getFotoUrl())
                .ativo(cliente.getAtivo())
                .criadoEm(cliente.getCriadoEm())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
