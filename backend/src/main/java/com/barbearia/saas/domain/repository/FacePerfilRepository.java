package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.FacePerfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade FacePerfil. */
public interface FacePerfilRepository extends JpaRepository<FacePerfil, Long> {
    Optional<FacePerfil> findByClienteIdAndAtivoTrue(Long clienteId);
    List<FacePerfil> findByBarbeariaIdAndAtivoTrue(Long barbeariaId);
}
