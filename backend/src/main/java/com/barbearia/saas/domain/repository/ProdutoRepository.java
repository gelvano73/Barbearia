package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade Produto. */
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(Long barbeariaId);

    List<Produto> findByBarbeariaIdOrderByNomeAsc(Long barbeariaId);

    Optional<Produto> findByIdAndBarbeariaId(Long id, Long barbeariaId);

    boolean existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrue(Long barbeariaId, String nome);

    boolean existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(Long barbeariaId, String nome, Long id);

    long countByBarbeariaId(Long barbeariaId);

    List<Produto> findByBarbeariaIdAndMarketplaceAtivoTrueAndAtivoTrueOrderByNomeAsc(Long barbeariaId);
}
