package com.firebolts.vetgestsbd.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; // Identificador único del rol en la base de datos

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_nombre", unique = true, nullable = false, length = 30)
    private RolTipo rol; // Tipo de rol definido como enumeración (ADMIN, VETERINARIO, PROPIETARIO)

    @Column(name = "descripcion", length = 100)
    private String descripcion; // Descripción breve del rol, por ejemplo "Administrador del sistema"
}