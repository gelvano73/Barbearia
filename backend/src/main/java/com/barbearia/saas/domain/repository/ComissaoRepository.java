package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Comissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Repositório Spring Data JPA para acesso à entidade Comissao. */
public interface ComissaoRepository extends JpaRepository<Comissao, Long> {
    boolean existsByAgendamentoId(Long agendamentoId);

    List<Comissao> findByBarbeiroIdAndCriadoEmBetweenOrderByCriadoEmDesc(
            Long barbeiroId, LocalDateTime inicio, LocalDateTime fim);

    List<Comissao> findByBarbeariaIdAndCriadoEmBetweenOrderByCriadoEmDesc(
            Long barbeariaId, LocalDateTime inicio, LocalDateTime fim);

    List<Comissao> findByBarbeariaIdAndBarbeiroIdAndCriadoEmBetweenOrderByCriadoEmDesc(
            Long barbeariaId, Long barbeiroId, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(c.valorComissao), 0) FROM Comissao c WHERE c.barbeiro.id = :barbeiroId AND c.criadoEm >= :inicio AND c.criadoEm < :fim")
    BigDecimal somarComissaoPeriodo(
            @Param("barbeiroId") Long barbeiroId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("""
            SELECT COALESCE(SUM(c.valorComissao), 0) FROM Comissao c
            WHERE c.barbearia.id = :barbeariaId AND c.criadoEm >= :inicio AND c.criadoEm < :fim
            """)
    BigDecimal somarComissaoBarbearia(
            @Param("barbeariaId") Long barbeariaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("""
            SELECT c.barbeiro.id, c.barbeiro.nome, COUNT(c), COALESCE(SUM(c.valorServico), 0), COALESCE(SUM(c.valorComissao), 0)
            FROM Comissao c
            WHERE c.barbearia.id = :barbeariaId AND c.criadoEm >= :inicio AND c.criadoEm < :fim
            GROUP BY c.barbeiro.id, c.barbeiro.nome
            ORDER BY SUM(c.valorComissao) DESC
            """)
    List<Object[]> rankingMensal(
            @Param("barbeariaId") Long barbeariaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
