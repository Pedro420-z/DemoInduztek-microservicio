package com.induztek.equipos.dto;

import java.math.BigDecimal;

public record EquipoRequest(
        Long instalacionId,
        String tag,
        Long tipoEquipoId,
        String marca,
        String modelo,
        String numeroSerie,
        Integer anioFabricacion,
        BigDecimal potenciaKva,
        BigDecimal tensionPrimariaKv,
        BigDecimal tensionSecundariaKv,
        BigDecimal corrienteNominalA,
        BigDecimal maxContactosUohm,
        String observaciones
) {
}
