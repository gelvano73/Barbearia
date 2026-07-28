package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.BarbeiroMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade BarbeiroMeta. */
public interface BarbeiroMetaRepository extends JpaRepository<BarbeiroMeta, Long> {
    Optional<BarbeiroMeta> findByBarbeiroIdAndAnoAndMes(Long barbeiroId, Integer ano, Integer mes);
}
