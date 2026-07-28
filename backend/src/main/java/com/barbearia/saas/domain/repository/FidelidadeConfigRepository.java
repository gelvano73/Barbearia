package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.FidelidadeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade FidelidadeConfig. */
public interface FidelidadeConfigRepository extends JpaRepository<FidelidadeConfig, Long> {
    Optional<FidelidadeConfig> findByBarbeariaId(Long barbeariaId);
}
