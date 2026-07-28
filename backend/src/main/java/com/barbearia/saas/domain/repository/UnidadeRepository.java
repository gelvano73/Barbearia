package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade Unidade. */
public interface UnidadeRepository extends JpaRepository<Unidade, Long> {
    List<Unidade> findByBarbeariaIdAndAtivoTrueOrderByPadraoDescNomeAsc(Long barbeariaId);

    List<Unidade> findByBarbeariaIdOrderByPadraoDescNomeAsc(Long barbeariaId);

    Optional<Unidade> findByIdAndBarbeariaId(Long id, Long barbeariaId);

    Optional<Unidade> findFirstByBarbeariaIdAndPadraoTrueAndAtivoTrue(Long barbeariaId);

    boolean existsByBarbeariaId(Long barbeariaId);

    boolean existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrue(Long barbeariaId, String nome);

    boolean existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(Long barbeariaId, String nome, Long id);
}
