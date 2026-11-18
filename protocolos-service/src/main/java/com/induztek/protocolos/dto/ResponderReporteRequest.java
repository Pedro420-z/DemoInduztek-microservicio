package com.induztek.protocolos.dto;

import com.induztek.protocolos.model.EstadoReporte;

public record ResponderReporteRequest(
        EstadoReporte estado,
        String respuesta
) {
}
