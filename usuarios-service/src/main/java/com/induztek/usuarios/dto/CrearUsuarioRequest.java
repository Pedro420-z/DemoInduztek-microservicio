package com.induztek.usuarios.dto;

public record CrearUsuarioRequest(
        String rut,
        String nombre,
        String correo,
        String telefono,
        Long rolId,
        String giro,
        String contactoNombre,
        String contactoCargo
) {
}
