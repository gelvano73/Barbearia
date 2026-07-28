package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Caixa;
import com.barbearia.saas.domain.enums.StatusCaixa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade Caixa. */
public interface CaixaRepository extends JpaRepository<Caixa, Long> {
    Optional<Caixa> findFirstByBarbeariaIdAndStatusOrderByAbertoEmDesc(Long barbeariaId, StatusCaixa status);
    Optional<Caixa> findByIdAndBarbeariaId(Long id, Long barbeariaId);
    List<Caixa> findByBarbeariaIdOrderByAbertoEmDesc(Long barbeariaId);
    boolean existsByBarbeariaIdAndStatus(Long barbeariaId, StatusCaixa status);
}
