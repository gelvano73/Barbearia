package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Barbeiro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade Barbeiro. */
public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {
    List<Barbeiro> findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(Long barbeariaId);
    List<Barbeiro> findByBarbeariaIdOrderByNomeAsc(Long barbeariaId);
    Optional<Barbeiro> findByIdAndBarbeariaId(Long id, Long barbeariaId);
    Optional<Barbeiro> findByUsuarioId(Long usuarioId);
}
