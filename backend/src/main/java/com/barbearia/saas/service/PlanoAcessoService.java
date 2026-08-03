package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.enums.PlanoAssinatura;
import com.barbearia.saas.domain.enums.PlanoRecurso;
import com.barbearia.saas.domain.enums.Role;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.BarbeiroRepository;
import com.barbearia.saas.domain.repository.ClienteRepository;
import com.barbearia.saas.domain.repository.UnidadeRepository;
import com.barbearia.saas.domain.repository.UsuarioRepository;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Regras comerciais de plano: limites quantitativos e recursos liberados.
 */
@Service
@RequiredArgsConstructor
public class PlanoAcessoService {

    private static final int ILIMITADO = Integer.MAX_VALUE;

    private final BarbeariaRepository barbeariaRepository;
    private final UnidadeRepository unidadeRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    /** === Consultas === */

    @Transactional(readOnly = true)
    public Barbearia barbeariaAtual() {
        Long id = SecurityUtils.getBarbeariaIdAtual();
        return barbeariaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
    }

    @Transactional(readOnly = true)
    public PlanoAssinatura planoAtual() {
        PlanoAssinatura plano = barbeariaAtual().getPlano();
        return plano != null ? plano : PlanoAssinatura.TRIAL;
    }

    /** TRIAL e ENTERPRISE: todos os recursos. */
    public boolean recursoLiberado(PlanoAssinatura plano, PlanoRecurso recurso) {
        if (plano == null || plano == PlanoAssinatura.TRIAL || plano == PlanoAssinatura.ENTERPRISE) {
            return true;
        }
        if (plano == PlanoAssinatura.BASIC) {
            return false;
        }
        // PRO
        return switch (recurso) {
            case PAGAMENTO_ONLINE, WHATSAPP, FIDELIDADE, ESTOQUE, COMISSOES,
                    CHECKIN, MARKETPLACE, BACKUP -> true;
            case NFSE, FRANQUIAS, IA_GESTAO -> false;
        };
    }

    @Transactional(readOnly = true)
    public void exigirRecurso(PlanoRecurso recurso) {
        exigirRecurso(SecurityUtils.getBarbeariaIdAtual(), recurso);
    }

    @Transactional(readOnly = true)
    public void exigirRecurso(Long barbeariaId, PlanoRecurso recurso) {
        Barbearia b = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        PlanoAssinatura plano = b.getPlano() != null ? b.getPlano() : PlanoAssinatura.TRIAL;
        if (!recursoLiberado(plano, recurso)) {
            throw new NegocioException(mensagemUpgrade(recurso, plano));
        }
    }

    @Transactional(readOnly = true)
    public boolean temRecurso(Long barbeariaId, PlanoRecurso recurso) {
        return barbeariaRepository.findById(barbeariaId)
                .map(b -> {
                    PlanoAssinatura plano = b.getPlano() != null ? b.getPlano() : PlanoAssinatura.TRIAL;
                    return recursoLiberado(plano, recurso);
                })
                .orElse(false);
    }

    public int maxUnidades(PlanoAssinatura plano) {
        return switch (plano == null ? PlanoAssinatura.TRIAL : plano) {
            case BASIC -> 1;
            case PRO -> 3;
            case TRIAL, ENTERPRISE -> ILIMITADO;
        };
    }

    public int maxBarbeiros(PlanoAssinatura plano) {
        return switch (plano == null ? PlanoAssinatura.TRIAL : plano) {
            case BASIC -> 3;
            case PRO -> 10;
            case TRIAL, ENTERPRISE -> ILIMITADO;
        };
    }

    public int maxClientes(PlanoAssinatura plano) {
        return switch (plano == null ? PlanoAssinatura.TRIAL : plano) {
            case BASIC -> 500;
            case PRO, TRIAL, ENTERPRISE -> ILIMITADO;
        };
    }

    public int maxAtendentes(PlanoAssinatura plano) {
        return switch (plano == null ? PlanoAssinatura.TRIAL : plano) {
            case BASIC -> 1;
            case PRO -> 3;
            case TRIAL, ENTERPRISE -> ILIMITADO;
        };
    }

    @Transactional(readOnly = true)
    public void exigirPodeCriarUnidade() {
        PlanoAssinatura plano = planoAtual();
        int max = maxUnidades(plano);
        if (max == ILIMITADO) {
            return;
        }
        long atuais = unidadeRepository.countByBarbeariaIdAndAtivoTrue(SecurityUtils.getBarbeariaIdAtual());
        if (atuais >= max) {
            throw new NegocioException(
                    "Limite de unidades do plano " + plano + " atingido (" + max + "). Faça upgrade em Assinatura.");
        }
    }

    @Transactional(readOnly = true)
    public void exigirPodeCriarBarbeiro() {
        PlanoAssinatura plano = planoAtual();
        int max = maxBarbeiros(plano);
        if (max == ILIMITADO) {
            return;
        }
        long atuais = barbeiroRepository.countByBarbeariaIdAndAtivoTrue(SecurityUtils.getBarbeariaIdAtual());
        if (atuais >= max) {
            throw new NegocioException(
                    "Limite de barbeiros do plano " + plano + " atingido (" + max + "). Faça upgrade em Assinatura.");
        }
    }

    @Transactional(readOnly = true)
    public void exigirPodeCriarCliente() {
        exigirPodeCriarCliente(SecurityUtils.getBarbeariaIdAtual());
    }

    @Transactional(readOnly = true)
    public void exigirPodeCriarCliente(Long barbeariaId) {
        Barbearia b = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        PlanoAssinatura plano = b.getPlano() != null ? b.getPlano() : PlanoAssinatura.TRIAL;
        int max = maxClientes(plano);
        if (max == ILIMITADO) {
            return;
        }
        long atuais = clienteRepository.countByBarbeariaIdAndAtivoTrue(barbeariaId);
        if (atuais >= max) {
            throw new NegocioException(
                    "Limite de clientes do plano " + plano + " atingido (" + max + "). Faça upgrade em Assinatura.");
        }
    }

    @Transactional(readOnly = true)
    public void exigirPodeCriarAtendente() {
        PlanoAssinatura plano = planoAtual();
        int max = maxAtendentes(plano);
        if (max == ILIMITADO) {
            return;
        }
        long atuais = usuarioRepository.countByBarbeariaIdAndRoleAndAtivoTrue(
                SecurityUtils.getBarbeariaIdAtual(), Role.ATENDENTE);
        if (atuais >= max) {
            throw new NegocioException(
                    "Limite de usuários de recepção do plano " + plano + " atingido (" + max
                            + "). Faça upgrade em Assinatura.");
        }
    }

    /** Resumo para a API de assinatura / front. */
    @Transactional(readOnly = true)
    public Map<String, Object> resumoAcesso() {
        PlanoAssinatura plano = planoAtual();
        Long bid = SecurityUtils.getBarbeariaIdAtual();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("plano", plano.name());
        m.put("maxUnidades", maxUnidades(plano) == ILIMITADO ? null : maxUnidades(plano));
        m.put("maxBarbeiros", maxBarbeiros(plano) == ILIMITADO ? null : maxBarbeiros(plano));
        m.put("maxClientes", maxClientes(plano) == ILIMITADO ? null : maxClientes(plano));
        m.put("maxAtendentes", maxAtendentes(plano) == ILIMITADO ? null : maxAtendentes(plano));
        m.put("unidadesUsadas", unidadeRepository.countByBarbeariaIdAndAtivoTrue(bid));
        m.put("barbeirosUsados", barbeiroRepository.countByBarbeariaIdAndAtivoTrue(bid));
        m.put("clientesUsados", clienteRepository.countByBarbeariaIdAndAtivoTrue(bid));
        m.put("atendentesUsados", usuarioRepository.countByBarbeariaIdAndRoleAndAtivoTrue(bid, Role.ATENDENTE));
        Set<String> recursos = new java.util.LinkedHashSet<>();
        for (PlanoRecurso r : PlanoRecurso.values()) {
            if (recursoLiberado(plano, r)) {
                recursos.add(r.name());
            }
        }
        m.put("recursos", recursos);
        return m;
    }

    private String mensagemUpgrade(PlanoRecurso recurso, PlanoAssinatura plano) {
        String min = switch (recurso) {
            case NFSE, FRANQUIAS, IA_GESTAO -> "ENTERPRISE";
            default -> "PRO";
        };
        return "Recurso " + recurso.name() + " não disponível no plano " + plano
                + ". Faça upgrade para " + min + " em Assinatura.";
    }
}
