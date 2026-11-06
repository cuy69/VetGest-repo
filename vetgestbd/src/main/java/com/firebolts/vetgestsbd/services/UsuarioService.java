package com.firebolts.vetgestsbd.services;

import java.util.List;

import com.firebolts.vetgestsbd.models.Usuario;

public interface UsuarioService {

    // Métodos CRUD del servicio de Usuario
    Usuario crearUsuario(Usuario usuario);
    Usuario getUsuarioById(Long id);
    List<Usuario> getTodosUsuarios();
    Usuario actualizarUsuario(Usuario usuario);
    void eliminarUsuario(Long id);
}