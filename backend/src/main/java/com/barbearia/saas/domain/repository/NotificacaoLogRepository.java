package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.NotificacaoLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositório Spring Data JPA para acesso à entidade NotificacaoLog. */
public interface NotificacaoLogRepository extends JpaRepository<NotificacaoLog, Long> {

    boolean existsByReferencia(String referencia);

    List<NotificacaoLog> findByBarbeariaIdOrderByCriadoEmDesc(Long barbeariaId);
}
