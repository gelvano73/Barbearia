package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.PedidoMarketplace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade PedidoMarketplace. */
public interface PedidoMarketplaceRepository extends JpaRepository<PedidoMarketplace, Long> {
    List<PedidoMarketplace> findByBarbeariaIdOrderByCriadoEmDesc(Long barbeariaId);
    Optional<PedidoMarketplace> findByIdAndBarbeariaId(Long id, Long barbeariaId);
}
