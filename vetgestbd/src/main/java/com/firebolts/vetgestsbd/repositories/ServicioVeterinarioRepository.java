package com.firebolts.vetgestsbd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.firebolts.vetgestsbd.models.ServicioVeterinario;

public interface ServicioVeterinarioRepository extends JpaRepository<ServicioVeterinario, Long> {

    List<ServicioVeterinario> findByActivoTrue(); // Solo servicios visibles (HU13)

    boolean existsByNombreIgnoreCase(String nombre); // Evita nombres duplicados al crear

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, long id); // Evita duplicados al actualizar
}