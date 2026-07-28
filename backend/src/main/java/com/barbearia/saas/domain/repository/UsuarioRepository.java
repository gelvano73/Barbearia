package com.barbearia.saas.domain.repository;

import com.barbearia.saas.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Repositório Spring Data JPA para acesso à entidade Usuario. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u JOIN FETCH u.barbearia WHERE u.email = :email")
    Optional<Usuario> findByEmail(@Param("email") String email);

    boolean existsByEmail(String email);
}
