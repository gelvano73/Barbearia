package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Repositório Spring Data JPA para acesso à entidade Avaliacao. */
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    boolean existsByAgendamentoId(Long agendamentoId);

    List<Avaliacao> findByBarbeiroIdOrderByCriadoEmDesc(Long barbeiroId);

    @Query("SELECT COALESCE(AVG(a.nota), 0) FROM Avaliacao a WHERE a.barbeiro.id = :barbeiroId")
    Double mediaPorBarbeiro(@Param("barbeiroId") Long barbeiroId);

    long countByBarbeiroId(Long barbeiroId);
}
