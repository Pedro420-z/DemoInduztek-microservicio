package com.induztek.protocolos.dto;

public record InstalacionExterno(
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
