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
import com.barbearia.saas.util.CpfUtil;
import com.barbearia.saas.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Cadastro e manutenção de clientes da barbearia (CRUD, foto e CPF
 * válido para tomador de NFS-e).
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final FotoStorageService fotoStorageService;
    private final EmailDominioService emailDominioService;
    private final PlanoAcessoService planoAcessoService;

    /** === Consultas === */

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(boolean apenasAtivos) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        List<Cliente> clientes = apenasAtivos
                ? clienteRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId)
                : clienteRepository.findByBarbeariaIdOrderByNomeAsc(barbeariaId);
        return clientes.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return toResponse(encontrarNaBarbearia(id));
    }

    /** === CRUD === */

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        planoAcessoService.exigirPodeCriarCliente();
        if (clienteRepository.existsByBarbeariaIdAndTelefoneAndAtivoTrue(barbeariaId, request.getTelefone())) {
            throw new NegocioException("Já existe cliente ativo com este telefone");
        }

        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        String cpf = normalizarCpfObrigatorio(request.getCpf());
        String email = blankToNull(request.getEmail());
        if (email != null) {
            emailDominioService.validarOuFalhar(email);
            email = EmailUtil.normalizar(email);
        }

        Cliente cliente = Cliente.builder()
                .barbearia(barbearia)
                .nome(request.getNome().trim())
                .telefone(request.getTelefone().trim())
                .email(email)
                .cpf(cpf)
                .observacoes(blankToNull(request.getObservacoes()))
                .ativo(true)
                .build();

        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = encontrarNaBarbearia(id);
        cliente.setNome(request.getNome().trim());
        cliente.setTelefone(request.getTelefone().trim());
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            emailDominioService.validarOuFalhar(request.getEmail());
            cliente.setEmail(EmailUtil.normalizar(request.getEmail()));
        } else {
            cliente.setEmail(null);
        }
        cliente.setObservacoes(blankToNull(request.getObservacoes()));
        if (request.getCpf() != null && !request.getCpf().isBlank()) {
            cliente.setCpf(normalizarCpfObrigatorio(request.getCpf()));
        }
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse uploadFoto(Long id, MultipartFile arquivo) {
        Cliente cliente = encontrarNaBarbearia(id);
        String url = fotoStorageService.salvar(arquivo, "clientes", "cliente-" + cliente.getId());
        cliente.setFotoUrl(url);
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void desativar(Long id) {
        Cliente cliente = encontrarNaBarbearia(id);
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    /** === Auxiliares === */

    private String normalizarCpfObrigatorio(String cpfRaw) {
        if (cpfRaw == null || cpfRaw.isBlank()) {
            throw new NegocioException("CPF do cliente é obrigatório (Receita Federal) para emissão de NFS-e");
        }
        String cpf = CpfUtil.somenteDigitos(cpfRaw);
        if (!CpfUtil.isValidoParaNotaFiscal(cpf, true)) {
            throw new NegocioException(
                    "CPF inválido ou de demonstração. Informe o CPF real do tomador conforme a Receita Federal.");
        }
        return cpf;
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
                .cpf(cliente.getCpf() != null ? CpfUtil.formatar(cliente.getCpf()) : null)
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
