package com.induztek.usuarios.dto;

public record UsuarioResponse(
        Long id,
        Long rolId,
        String rolCodigo,
        String rolNombre,
        String rut,
        String nombre,
        String correo,
        String telefono,
        boolean activo,
        String giro,
        String contactoNombre,
        String contactoCargo,
        boolean debeCambiarContrasena
) {
}
