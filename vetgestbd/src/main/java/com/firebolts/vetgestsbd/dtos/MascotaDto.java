package com.firebolts.vetgestsbd.dtos;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MascotaDto {

    private Long id;

    @NotBlank(message = "El nombre de la mascota es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @NotBlank(message = "La especie es obligatoria")
    private String especie;

    @Size(max = 50, message = "La raza no debe superar los 50 caracteres")
    private String raza;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^(?i)(MACHO|HEMBRA)$", message = "El sexo debe ser MACHO o HEMBRA")
    private String sexo;

    @Positive(message = "El peso debe ser mayor a 0")
    private Double peso;

    private String foto;
    private String color;
    private String notes;

    // Opcional: si un ADMIN registra la mascota para un cliente específico
    private Long propietarioId;

    @JsonProperty("name")
    public void setName(String name) { this.nombre = name; }

    @JsonProperty("species")
    public void setSpecies(String species) { this.especie = species; }

    @JsonProperty("breed")
    public void setBreed(String breed) { this.raza = breed; }

    @JsonProperty("birth_date")
    public void setBirthDate(LocalDate birth_date) { this.fechaNacimiento = birth_date; }

    @JsonProperty("weight")
    public void setWeight(Double weight) { this.peso = weight; }

    @JsonProperty("color")
    public void setColor(String color) {
        this.color = color;
        if (this.foto == null || this.foto.isBlank()) this.foto = color;
    }

    @JsonProperty("notes")
    public void setNotes(String notes) { this.notes = notes; }

    @JsonProperty("sex")
    public void setSex(String sex) {
        if (sex != null) {
            this.sexo = sex.toUpperCase();
        }
    }
}
