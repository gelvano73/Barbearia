package com.barbearia.saas.security;

import com.barbearia.saas.domain.enums.Role;
import com.barbearia.saas.domain.repository.BarbeiroRepository;
import com.barbearia.saas.domain.repository.ClienteRepository;
import com.barbearia.saas.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carrega o usuário autenticado a partir do repositório para o Spring Security. */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;

    /** Carrega o usuário pelo e-mail para autenticação. */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(username)
                .map(usuario -> {
                    Long clienteId = null;
                    Long barbeiroId = null;
                    if (usuario.getRole() == Role.CLIENTE) {
                        clienteId = clienteRepository.findByUsuarioId(usuario.getId())
                                .map(c -> c.getId())
                                .orElse(null);
                    } else if (usuario.getRole() == Role.BARBEIRO) {
                        barbeiroId = barbeiroRepository.findByUsuarioId(usuario.getId())
                                .map(b -> b.getId())
                                .orElse(null);
                    }
                    return new UsuarioPrincipal(usuario, clienteId, barbeiroId);
                })
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
    }
}
