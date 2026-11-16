package com.induztek.equipos.dto;

public record InstalacionRequest(
        Long clienteId,
        String nombre,
        String ubicacion,
        Long comunaId,
        String observacionesAcceso
) {
}
