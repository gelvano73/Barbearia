package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.BarbeiroHorario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade BarbeiroHorario. */
public interface BarbeiroHorarioRepository extends JpaRepository<BarbeiroHorario, Long> {
    List<BarbeiroHorario> findByBarbeiroIdOrderByDiaSemanaAsc(Long barbeiroId);
    Optional<BarbeiroHorario> findByBarbeiroIdAndDiaSemana(Long barbeiroId, Integer diaSemana);
    void deleteByBarbeiroId(Long barbeiroId);
}
