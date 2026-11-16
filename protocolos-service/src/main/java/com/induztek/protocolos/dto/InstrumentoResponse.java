package com.induztek.protocolos.dto;

import java.time.LocalDate;

public record InstrumentoResponse(
        Long id,
        String nombre,
        String marca,
        String modelo,
        String numeroSerie,
        LocalDate ultimaCalibracion,
        LocalDate vencimientoCalibracion,
        boolean vigente
) {
}
