package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.EstoqueMovimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositório Spring Data JPA para acesso à entidade EstoqueMovimento. */
public interface EstoqueMovimentoRepository extends JpaRepository<EstoqueMovimento, Long> {
    List<EstoqueMovimento> findByBarbeariaIdOrderByCriadoEmDesc(Long barbeariaId);

    List<EstoqueMovimento> findByProdutoIdOrderByCriadoEmDesc(Long produtoId);
}
