package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.enums.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade Barbearia. */
public interface BarbeariaRepository extends JpaRepository<Barbearia, Long> {
    Optional<Barbearia> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
    List<Barbearia> findByEmpresaIdOrderByNomeAsc(Long empresaId);
    List<Barbearia> findByAtivoTrueOrderByNomeAsc();
    List<Barbearia> findByAssinaturaStatusAndAssinaturaVenceEmBefore(StatusAssinatura status, LocalDateTime limite);
}
