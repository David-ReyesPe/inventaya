package com.inventaya.inventario.config;

public record UsuarioAutenticado(String subject, RolUsuario rol) {

    /** Nombre del atributo de request donde el filtro JWT deja al usuario autenticado. */
    public static final String ATRIBUTO_REQUEST = "inventaya.usuarioAutenticado";
}
