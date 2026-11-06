package com.firebolts.vetgestsbd.models;

public enum RolTipo {

    CLIENTE("ROL_CLIENTE"), // Rol asignado a propietarios de mascotas (clientes de la veterinaria)
    ADMIN("ROL_ADMIN");     // Rol asignado a administradores del sistema

    private final String valor; // Valor interno asociado a cada tipo de rol

    RolTipo(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return this.valor; // Útil para autenticación o JWT
    }
}