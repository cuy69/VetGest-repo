package com.firebolts.vetgestsbd.services;

import java.util.List;

import com.firebolts.vetgestsbd.models.ServicioVeterinario;

public interface ServicioVeterinarioService {

    ServicioVeterinario crearServicio(ServicioVeterinario servicio);
    ServicioVeterinario getServicioById(Long id);
    List<ServicioVeterinario> getServiciosActivos();   // HU13 (clientes)
    List<ServicioVeterinario> getTodosServicios();     // Administración
    ServicioVeterinario actualizarServicio(ServicioVeterinario servicio);
    ServicioVeterinario cambiarEstado(Long id, boolean activo); // Activar o desactivar
}