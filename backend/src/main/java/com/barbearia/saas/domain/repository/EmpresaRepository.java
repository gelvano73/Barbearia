package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositório Spring Data JPA para acesso à entidade Empresa. */
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    List<Empresa> findByAtivoTrueOrderByNomeAsc();
    List<Empresa> findAllByOrderByNomeAsc();
    boolean existsByCnpj(String cnpj);
}
