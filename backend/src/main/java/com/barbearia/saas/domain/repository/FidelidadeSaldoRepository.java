package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.FidelidadeSaldo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade FidelidadeSaldo. */
public interface FidelidadeSaldoRepository extends JpaRepository<FidelidadeSaldo, Long> {
    Optional<FidelidadeSaldo> findByClienteId(Long clienteId);

    List<FidelidadeSaldo> findByBarbeariaIdOrderByPontosDesc(Long barbeariaId);
}
