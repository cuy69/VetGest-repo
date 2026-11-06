package com.firebolts.vetgestsbd.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.firebolts.vetgestsbd.models.Rol;
import com.firebolts.vetgestsbd.models.RolTipo;

public interface RolRepository extends JpaRepository<Rol, Long> {

    // Buscar el Rol por su enumeración (CLIENTE o ADMIN)
    Optional<Rol> findByRol(RolTipo rol);

    // Verificar si un rol ya existe (útil para crear los roles al arrancar la app)
    boolean existsByRol(RolTipo rol);
}