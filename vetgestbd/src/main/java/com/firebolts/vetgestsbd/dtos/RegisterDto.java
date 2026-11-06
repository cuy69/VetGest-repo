package com.firebolts.vetgestsbd.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.firebolts.vetgestsbd.models.RolTipo;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no debe superar los 50 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 50, message = "El apellido no debe superar los 50 caracteres")
    private String apellido;

    @Size(max = 30, message = "El nombre de usuario no debe tener más de 30 caracteres")
    private String username;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Debe ingresar un correo válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 60, message = "La contraseña debe tener entre 6 y 60 caracteres")
    private String password;

    @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,25}$", message = "Número de teléfono inválido")
    private String telefono;

    @Size(max = 100, message = "La dirección no debe superar los 100 caracteres")
    private String direccion;

    // Documento de identidad, opcional. Vacío o null significa que no se registra
    @Pattern(regexp = "^$|^[0-9A-Za-z]{6,15}$", message = "El documento debe tener entre 6 y 15 caracteres alfanuméricos")
    private String documento;

    // Rol del usuario (por defecto CLIENTE)
    private RolTipo rol = RolTipo.CLIENTE;

    // Soporte para atributos del frontend React
    @JsonProperty("first_name")
    public void setFirstName(String first_name) {
        this.nombre = first_name;
    }

    @JsonProperty("last_name")
    public void setLastName(String last_name) {
        this.apellido = last_name;
    }

    @JsonProperty("phone")
    public void setPhone(String phone) {
        this.telefono = phone;
    }

    @JsonProperty("password_confirmation")
    public void setPasswordConfirmation(String password_confirmation) {
        // Validación frontend
    }
}
