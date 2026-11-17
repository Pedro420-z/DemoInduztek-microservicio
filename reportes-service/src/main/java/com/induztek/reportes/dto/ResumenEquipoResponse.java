package com.induztek.reportes.dto;

import java.time.LocalDateTime;

public record ResumenEquipoResponse(
        EquipoExterno equipo,
        int totalProtocolos,
        String ultimoNumero,
        LocalDateTime ultimaFecha,
        String ultimoResultado
) {
}
