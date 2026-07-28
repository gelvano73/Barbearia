package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade PasswordResetToken. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenAndUsadoFalse(String token);
}
