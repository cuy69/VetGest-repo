package com.firebolts.vetgestsbd.models;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; // Identificador único del usuario en la base de datos

    @Column(nullable = false, length = 50)
    @NotBlank
    @Size(min = 2, message = "El nombre debe tener al menos 2 caracteres")
    @Pattern(regexp = "^[\\p{L} \\-']+$", message = "El nombre solo puede contener letras, espacios, guiones o apóstrofes")
    private String nombre; // Nombre del usuario/propietario, campo obligatorio

    @Column(nullable = false, length = 50)
    @NotBlank
    @Size(min = 2, message = "El apellido debe tener al menos 2 caracteres")
    @Pattern(regexp = "^[\\p{L} \\-']+$", message = "El apellido solo puede contener letras, espacios, guiones o apóstrofes")
    private String apellido; // Apellido del usuario/propietario, campo obligatorio

    @Column(nullable = false, length = 30, unique = true)
    @NotBlank
    private String username; // Nombre de usuario único utilizado para autenticación

    @Column(nullable = false, length = 50, unique = true)
    @NotBlank
    @Email
    private String email; // Correo electrónico único, validado con formato de email

    @Column(nullable = false, length = 60)
    @NotBlank
    @Size(min = 6, max = 60)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // evita mostrar contraseña al serializar JSON
    private String password; // Contraseña del usuario, encriptada al almacenarse

    @Column(length = 15)
    @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$") // acepta vacío o + opcional y entre 9 y 15 dígitos
    private String telefono; // Teléfono del propietario, campo opcional

    @Column(length = 100)
    private String direccion; // Dirección física del propietario, campo opcional

    @Column(length = 15, unique = true)
    @Pattern(regexp = "^[0-9A-Za-z]{6,15}$", message = "El documento debe tener entre 6 y 15 caracteres alfanuméricos")
    private String documento; // Documento de identidad del propietario (DNI, CE, etc.), campo opcional y único

    @Column(name = "photo", columnDefinition = "LONGTEXT")
    private String profilePhoto; // Foto de perfil en formato base64, campo opcional

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles; // Un usuario puede tener varios roles (cliente, administrador)

    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL)
    @JsonIgnore // evita recursión infinita al convertir a JSON
    private List<Mascota> mascotas; // Mascotas del propietario

    // Convierte documento vacío en null para no chocar con la restricción unique
    public void setDocumento(String documento) {
        this.documento = (documento == null || documento.isBlank()) ? null : documento.trim();
    }
}