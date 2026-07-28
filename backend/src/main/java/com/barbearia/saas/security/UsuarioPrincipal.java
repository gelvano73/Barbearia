package com.barbearia.saas.security;

import com.barbearia.saas.domain.entity.Usuario;
import com.barbearia.saas.domain.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Implementação de UserDetails com dados do usuário e da barbearia (tenant). */
@Getter
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final Long barbeariaId;
    private final Long clienteId;
    private final Long barbeiroId;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final Role role;
    private final boolean ativo;

    public UsuarioPrincipal(Usuario usuario, Long clienteId, Long barbeiroId) {
        this.id = usuario.getId();
        this.barbeariaId = usuario.getBarbearia().getId();
        this.clienteId = clienteId;
        this.barbeiroId = barbeiroId;
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash();
        this.role = usuario.getRole();
        this.ativo = Boolean.TRUE.equals(usuario.getAtivo());
    }

    public UsuarioPrincipal(Usuario usuario, Long clienteId) {
        this(usuario, clienteId, null);
    }

    public UsuarioPrincipal(Usuario usuario) {
        this(usuario, null, null);
    }

    /** Retorna as authorities Spring Security do usuário. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** Retorna o hash da senha. */
    @Override
    public String getPassword() {
        return senhaHash;
    }

    /** Retorna o e-mail usado como username. */
    @Override
    public String getUsername() {
        return email;
    }

    /** Indica se a conta não está expirada. */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Indica se a conta não está bloqueada (usuário ativo). */
    @Override
    public boolean isAccountNonLocked() {
        return ativo;
    }

    /** Indica se as credenciais não estão expiradas. */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Indica se o usuário está habilitado. */
    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
