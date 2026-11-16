package com.induztek.usuarios.dto;

public record ActualizarPerfilRequest(
        String nombre,
        String correo,
        String telefono,
        String giro,
        String contactoNombre,
        String contactoCargo
) {
}
