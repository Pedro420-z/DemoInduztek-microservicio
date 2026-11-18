package com.induztek.protocolos.dto;

import java.math.BigDecimal;

public record EquipoNuevo(
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
