package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.NotaFiscal;
import com.barbearia.saas.domain.enums.StatusNotaFiscal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório JPA de notas fiscais de serviço (NFS-e) por barbearia e pagamento. */
public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Long> {

    Optional<NotaFiscal> findByPagamentoId(Long pagamentoId);

    Optional<NotaFiscal> findByIdAndBarbeariaId(Long id, Long barbeariaId);

    List<NotaFiscal> findByBarbeariaIdOrderByCriadoEmDesc(Long barbeariaId);

    List<NotaFiscal> findByBarbeariaIdAndStatusOrderByCriadoEmDesc(Long barbeariaId, StatusNotaFiscal status);

    boolean existsByPagamentoId(Long pagamentoId);
}
