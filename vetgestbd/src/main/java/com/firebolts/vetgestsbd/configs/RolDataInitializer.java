package com.firebolts.vetgestsbd.configs;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.firebolts.vetgestsbd.models.Rol;
import com.firebolts.vetgestsbd.models.RolTipo;
import com.firebolts.vetgestsbd.models.ServicioVeterinario;
import com.firebolts.vetgestsbd.repositories.RolRepository;
import com.firebolts.vetgestsbd.repositories.ServicioVeterinarioRepository;

@Configuration
public class RolDataInitializer {

    // Se ejecuta una vez cuando la aplicación está lista
    @Bean
    public CommandLineRunner initRoles(RolRepository rolRepository,
                                       ServicioVeterinarioRepository servicioRepository) {
        return args -> {

            // 1. Verificar si rol CLIENTE existe
            rolRepository.findByRol(RolTipo.CLIENTE).orElseGet(() -> {
                Rol cliente = new Rol();
                cliente.setRol(RolTipo.CLIENTE);
                cliente.setDescripcion("Rol por defecto para usuarios registrados.");
                return rolRepository.save(cliente);
            });

            // 2. Verificar si rol ADMIN existe
            rolRepository.findByRol(RolTipo.ADMIN).orElseGet(() -> {
                Rol admin = new Rol();
                admin.setRol(RolTipo.ADMIN);
                admin.setDescripcion("Rol para administradores del sistema.");
                return rolRepository.save(admin);
            });

            // 3. Verificar si existen servicios iniciales
            if (servicioRepository.count() == 0) {
                servicioRepository.save(new ServicioVeterinario(0, "Consulta general", "Evaluación física y orientación sobre el bienestar de tu mascota.", new BigDecimal("65.00"), 30, true, null));
                servicioRepository.save(new ServicioVeterinario(0, "Vacunación", "Evaluación previa y aplicación de la vacuna indicada por el profesional. Precio según vacuna.", new BigDecimal("55.00"), 20, true, null));
                servicioRepository.save(new ServicioVeterinario(0, "Desparasitación", "Evaluación y elección de un antiparasitario según especie, edad y peso.", new BigDecimal("35.00"), 20, true, null));
                servicioRepository.save(new ServicioVeterinario(0, "Control", "Revisión de la evolución y recomendaciones de cuidado para tu mascota.", new BigDecimal("40.00"), 20, true, null));
                System.out.println("Servicios veterinarios iniciales cargados en base de datos.");
            }
        };
    }
}