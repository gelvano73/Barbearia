package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.OAuthIdentity;
import com.barbearia.saas.domain.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade OAuthIdentity. */
public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, Long> {
    Optional<OAuthIdentity> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
