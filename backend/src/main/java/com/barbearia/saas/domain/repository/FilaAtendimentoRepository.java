package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.FilaAtendimento;
import com.barbearia.saas.domain.enums.StatusFila;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade FilaAtendimento. */
public interface FilaAtendimentoRepository extends JpaRepository<FilaAtendimento, Long> {

    List<FilaAtendimento> findByBarbeariaIdAndStatusInOrderByPrioridadeDescPosicaoAsc(
            Long barbeariaId, List<StatusFila> status);

    Optional<FilaAtendimento> findByIdAndBarbeariaId(Long id, Long barbeariaId);

    @Query("SELECT COALESCE(MAX(f.posicao), 0) FROM FilaAtendimento f WHERE f.barbearia.id = :barbeariaId AND f.status IN :status")
    Integer maxPosicaoAtiva(@Param("barbeariaId") Long barbeariaId, @Param("status") List<StatusFila> status);

    long countByBarbeariaIdAndStatus(Long barbeariaId, StatusFila status);
}
