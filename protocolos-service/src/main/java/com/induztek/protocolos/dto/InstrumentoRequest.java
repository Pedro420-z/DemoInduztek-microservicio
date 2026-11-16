package com.induztek.protocolos.dto;

public record InstrumentoRequest(
        String nombre,
        String marca,
        String modelo,
        String numeroSerie
) {
}
