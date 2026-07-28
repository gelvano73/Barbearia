package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Agendamento;
import com.barbearia.saas.domain.enums.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade Agendamento. */
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findByBarbeariaIdOrderByDataHoraDesc(Long barbeariaId);

    Optional<Agendamento> findByIdAndBarbeariaId(Long id, Long barbeariaId);

    List<Agendamento> findByBarbeariaIdAndDataHoraBetweenOrderByDataHoraAsc(
            Long barbeariaId, LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByBarbeariaIdAndBarbeiroIdAndDataHoraBetweenOrderByDataHoraAsc(
            Long barbeariaId, Long barbeiroId, LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByClienteIdOrderByDataHoraDesc(Long clienteId);

    List<Agendamento> findByClienteIdAndStatusOrderByDataHoraDesc(Long clienteId, StatusAgendamento status);

    Optional<Agendamento> findByIdAndClienteId(Long id, Long clienteId);

    List<Agendamento> findByBarbeiroIdAndDataHoraBetweenOrderByDataHoraAsc(
            Long barbeiroId, LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByBarbeiroIdAndStatusOrderByDataHoraDesc(Long barbeiroId, StatusAgendamento status);

    long countByBarbeiroIdAndStatusAndDataHoraBetween(
            Long barbeiroId, StatusAgendamento status, LocalDateTime inicio, LocalDateTime fim);

    Optional<Agendamento> findByIdAndBarbeiroId(Long id, Long barbeiroId);

    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.barbeiro.id = :barbeiroId
              AND a.status NOT IN :statusExcluidos
              AND a.dataHora < :janelaFim
              AND a.dataHora > :janelaInicio
              AND (:agendamentoId IS NULL OR a.id <> :agendamentoId)
            """)
    List<Agendamento> findCandidatosConflito(
            @Param("barbeiroId") Long barbeiroId,
            @Param("janelaInicio") LocalDateTime janelaInicio,
            @Param("janelaFim") LocalDateTime janelaFim,
            @Param("statusExcluidos") List<StatusAgendamento> statusExcluidos,
            @Param("agendamentoId") Long agendamentoId);
}
