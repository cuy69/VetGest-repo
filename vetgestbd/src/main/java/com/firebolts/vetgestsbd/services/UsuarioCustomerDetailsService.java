package com.firebolts.vetgestsbd.services;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.firebolts.vetgestsbd.models.Usuario;
import com.firebolts.vetgestsbd.repositories.UsuarioRepository;

@Service
public class UsuarioCustomerDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository; // Inyección del repositorio de usuarios

    public UsuarioCustomerDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Carga un usuario desde la base de datos por su nombre de usuario.
     * Devuelve un objeto UserDetails compatible con Spring Security.
     *
     * @param username nombre de usuario ingresado durante la autenticación
     * @return UserDetails con username, contraseña y roles del usuario
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword()) // Contraseña encriptada
                .authorities(
                        usuario.getRoles().stream()
                                // Prefijo ROLE_ para que funcionen hasRole()/hasAnyRole()
                                .map(rol -> "ROLE_" + rol.getRol().name())
                                .toArray(String[]::new)
                )
                .build();
    }
}