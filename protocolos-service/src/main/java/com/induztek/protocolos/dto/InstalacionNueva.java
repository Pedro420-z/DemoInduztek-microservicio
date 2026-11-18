package com.induztek.protocolos.dto;

public record InstalacionNueva(
        Long clienteId,
        String nombre,
        String ubicacion,
        Long comunaId,
        String observacionesAcceso
) {
}
