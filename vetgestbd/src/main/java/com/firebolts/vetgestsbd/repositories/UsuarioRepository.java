package com.firebolts.vetgestsbd.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.firebolts.vetgestsbd.models.RolTipo;
import com.firebolts.vetgestsbd.models.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar por username
    Optional<Usuario> findByUsername(String username);

    // Buscar por email
    Optional<Usuario> findByEmail(String email);

    // Buscar por documento de identidad
    Optional<Usuario> findByDocumento(String documento);

    // Buscar todos los usuarios con rol específico (CLIENTE o ADMIN) (HU07)
    @Query("SELECT u FROM Usuario u JOIN u.roles r WHERE r.rol = :rolTipo")
    List<Usuario> findByRolTipo(@Param("rolTipo") RolTipo rolTipo);

    // Buscar usuarios que tengan mascotas registradas
    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.mascotas m")
    List<Usuario> findUsuariosConMascotas();

    // Buscar usuarios cuyo nombre o apellido contenga cierta palabra (buscador)
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.nombre) LIKE LOWER(CONCAT('%', :palabra, '%')) OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :palabra, '%'))")
    List<Usuario> buscarPorNombreOApellido(@Param("palabra") String palabra);

    // Verificar duplicados en registro
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByDocumento(String documento);
}