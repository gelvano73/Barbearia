package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.WhatsappSessao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade WhatsappSessao. */
public interface WhatsappSessaoRepository extends JpaRepository<WhatsappSessao, Long> {
    Optional<WhatsappSessao> findByBarbeariaIdAndTelefone(Long barbeariaId, String telefone);
}
