package com.induztek.equipos.dto;

public record ComunaResponse(
        Long id,
        String nombre,
        Long regionId,
        String regionNombre
) {
}
