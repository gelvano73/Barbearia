package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.BarbeiroFerias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade BarbeiroFerias. */
public interface BarbeiroFeriasRepository extends JpaRepository<BarbeiroFerias, Long> {
    List<BarbeiroFerias> findByBarbeiroIdOrderByDataInicioDesc(Long barbeiroId);
    Optional<BarbeiroFerias> findByIdAndBarbeiroId(Long id, Long barbeiroId);
}
