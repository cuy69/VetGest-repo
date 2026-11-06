package com.firebolts.vetgestsbd.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SolicitarCitaDto {

    @NotNull(message = "El ID de la mascota es obligatorio")
    private Long mascotaId;

    @NotNull(message = "El ID del servicio es obligatorio")
    private Long servicioId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
    private String motivo;

    private Long clinicId;

    @JsonProperty("pet_id")
    public void setPetId(Long pet_id) {
        this.mascotaId = pet_id;
    }

    @JsonProperty("service_id")
    public void setServiceId(Long service_id) {
        this.servicioId = service_id;
    }

    @JsonProperty("clinic_id")
    public void setClinicId(Long clinic_id) {
        this.clinicId = clinic_id;
    }

    @JsonProperty("reason")
    public void setReason(String reason) {
        this.motivo = reason;
    }

    @JsonProperty("starts_at")
    public void setStartsAt(String starts_at) {
        if (starts_at != null && !starts_at.isBlank()) {
            try {
                java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse(starts_at);
                this.fecha = zdt.toLocalDate();
                this.hora = zdt.toLocalTime().withSecond(0).withNano(0);
            } catch (Exception e) {
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(starts_at);
                    this.fecha = ldt.toLocalDate();
                    this.hora = ldt.toLocalTime().withSecond(0).withNano(0);
                } catch (Exception ignored) {}
            }
        }
    }
}
