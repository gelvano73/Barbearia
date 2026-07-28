package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Pagamento;
import com.barbearia.saas.domain.enums.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade Pagamento. */
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByBarbeariaIdAndCriadoEmBetweenOrderByCriadoEmDesc(
            Long barbeariaId, LocalDateTime inicio, LocalDateTime fim);

    List<Pagamento> findByBarbeariaIdAndDataPagamentoOrderByCriadoEmDesc(Long barbeariaId, LocalDate data);

    List<Pagamento> findByCaixaIdOrderByCriadoEmDesc(Long caixaId);

    Optional<Pagamento> findByIdAndBarbeariaId(Long id, Long barbeariaId);

    @Query("""
            SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p
            WHERE p.barbearia.id = :barbeariaId AND p.status = :status
              AND p.dataPagamento >= :inicio AND p.dataPagamento <= :fim
            """)
    BigDecimal somarFaturamento(
            @Param("barbeariaId") Long barbeariaId,
            @Param("status") StatusPagamento status,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    @Query("""
            SELECT p.dataPagamento, COUNT(p), COALESCE(SUM(p.valor), 0)
            FROM Pagamento p
            WHERE p.barbearia.id = :barbeariaId AND p.status = :status
              AND p.dataPagamento >= :inicio AND p.dataPagamento <= :fim
            GROUP BY p.dataPagamento
            ORDER BY p.dataPagamento
            """)
    List<Object[]> faturamentoPorDia(
            @Param("barbeariaId") Long barbeariaId,
            @Param("status") StatusPagamento status,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    @Query("""
            SELECT p.servico.id, p.servico.nome, COUNT(p), COALESCE(SUM(p.valor), 0)
            FROM Pagamento p
            WHERE p.barbearia.id = :barbeariaId AND p.status = :status
              AND p.dataPagamento >= :inicio AND p.dataPagamento <= :fim
              AND p.servico IS NOT NULL
            GROUP BY p.servico.id, p.servico.nome
            ORDER BY COUNT(p) DESC, SUM(p.valor) DESC
            """)
    List<Object[]> servicosMaisVendidos(
            @Param("barbeariaId") Long barbeariaId,
            @Param("status") StatusPagamento status,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    @Query("""
            SELECT p.cliente.id, p.cliente.nome, COUNT(p), COALESCE(SUM(p.valor), 0)
            FROM Pagamento p
            WHERE p.barbearia.id = :barbeariaId AND p.status = :status
              AND p.dataPagamento >= :inicio AND p.dataPagamento <= :fim
              AND p.cliente IS NOT NULL
            GROUP BY p.cliente.id, p.cliente.nome
            ORDER BY COUNT(p) DESC, SUM(p.valor) DESC
            """)
    List<Object[]> clientesMaisFrequentes(
            @Param("barbeariaId") Long barbeariaId,
            @Param("status") StatusPagamento status,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);
}
