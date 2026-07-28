package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.MovimentoCaixa;
import com.barbearia.saas.domain.enums.TipoMovimentoCaixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/** Repositório Spring Data JPA para acesso à entidade MovimentoCaixa. */
public interface MovimentoCaixaRepository extends JpaRepository<MovimentoCaixa, Long> {
    List<MovimentoCaixa> findByCaixaIdOrderByCriadoEmDesc(Long caixaId);

    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM MovimentoCaixa m WHERE m.caixa.id = :caixaId AND m.tipo = :tipo")
    BigDecimal somarPorTipo(@Param("caixaId") Long caixaId, @Param("tipo") TipoMovimentoCaixa tipo);
}
