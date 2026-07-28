package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade Servico. */
public interface ServicoRepository extends JpaRepository<Servico, Long> {
    List<Servico> findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(Long barbeariaId);

    List<Servico> findByBarbeariaIdOrderByNomeAsc(Long barbeariaId);

    Optional<Servico> findByIdAndBarbeariaId(Long id, Long barbeariaId);

    boolean existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrue(Long barbeariaId, String nome);

    boolean existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(Long barbeariaId, String nome, Long id);
}
