package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.FidelidadeMovimento;
import com.barbearia.saas.domain.enums.TipoFidelidadeMovimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositório Spring Data JPA para acesso à entidade FidelidadeMovimento. */
public interface FidelidadeMovimentoRepository extends JpaRepository<FidelidadeMovimento, Long> {
    List<FidelidadeMovimento> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    boolean existsByAgendamentoIdAndTipo(Long agendamentoId, TipoFidelidadeMovimento tipo);
}
