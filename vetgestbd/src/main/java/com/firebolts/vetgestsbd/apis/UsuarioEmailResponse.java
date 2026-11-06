package com.firebolts.vetgestsbd.apis;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UsuarioEmailResponse {

    private String email; // Email verificado

    @JsonProperty("format_valid")
    private boolean formatoCorrecto;

    @JsonProperty("smtp_check")
    private boolean direccionExistente;

    private double score; // Nombre real usado por la API

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isFormatoCorrecto() {
        return formatoCorrecto;
    }

    public void setFormatoCorrecto(boolean formatoCorrecto) {
        this.formatoCorrecto = formatoCorrecto;
    }

    public boolean isDireccionExistente() {
        return direccionExistente;
    }

    public void setDireccionExistente(boolean direccionExistente) {
        this.direccionExistente = direccionExistente;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}