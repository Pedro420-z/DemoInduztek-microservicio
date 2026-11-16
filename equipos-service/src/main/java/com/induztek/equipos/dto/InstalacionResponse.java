package com.induztek.equipos.dto;

public record InstalacionResponse(
        Long id,
        Long clienteId,
        String nombre,
        String ubicacion,
        Long comunaId,
        String comunaNombre,
        Long regionId,
        String regionNombre,
        String observacionesAcceso
) {
}
