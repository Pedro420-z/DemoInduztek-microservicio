package com.induztek.reportes.dto;

public record UsuarioExterno(
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
