package com.firebolts.vetgestsbd.models;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; // Identificador único de la cita

    @Column(nullable = false)
    @NotNull(message = "La fecha de la cita es obligatoria")
    private LocalDate fecha; // Día de la cita

    @Column(nullable = false)
    @NotNull(message = "La hora de la cita es obligatoria")
    private LocalTime hora; // Hora de inicio de la cita

    @Column(length = 300)
    @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
    private String motivo; // Motivo o comentario del cliente, campo opcional

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCita estado = EstadoCita.PENDIENTE; // Estado actual de la cita

    @NotNull(message = "La cita debe tener una mascota")
    @ManyToOne(optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota; // Mascota que será atendida (de ella se obtiene el propietario)

    @NotNull(message = "La cita debe tener un servicio")
    @ManyToOne(optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private ServicioVeterinario servicio; // Servicio solicitado
}