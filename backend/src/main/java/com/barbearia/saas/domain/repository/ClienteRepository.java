package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade Cliente. */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(Long barbeariaId);
    List<Cliente> findByBarbeariaIdOrderByNomeAsc(Long barbeariaId);
    Optional<Cliente> findByIdAndBarbeariaId(Long id, Long barbeariaId);
    Optional<Cliente> findByUsuarioId(Long usuarioId);
    Optional<Cliente> findFirstByBarbeariaIdAndTelefoneAndAtivoTrue(Long barbeariaId, String telefone);
    boolean existsByBarbeariaIdAndTelefoneAndAtivoTrue(Long barbeariaId, String telefone);
}
