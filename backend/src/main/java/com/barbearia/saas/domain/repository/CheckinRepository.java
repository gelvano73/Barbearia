package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Checkin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/** Repositório Spring Data JPA para acesso à entidade Checkin. */
public interface CheckinRepository extends JpaRepository<Checkin, Long> {
    List<Checkin> findByBarbeariaIdAndCriadoEmBetweenOrderByCriadoEmDesc(
            Long barbeariaId, LocalDateTime inicio, LocalDateTime fim);
}
