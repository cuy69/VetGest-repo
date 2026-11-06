package com.firebolts.vetgestsbd.models;

public enum EstadoCita {
    PENDIENTE,   // Recién solicitada por el cliente (HU12)
    CONFIRMADA,  // Aprobada por la veterinaria
    COMPLETADA,  // Ya fue atendida
    CANCELADA    // Cancelada por el cliente o la veterinaria
}