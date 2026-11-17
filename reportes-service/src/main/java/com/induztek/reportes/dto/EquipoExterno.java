package com.induztek.reportes.dto;

import java.math.BigDecimal;

public record EquipoExterno(
        Long id,
        Long instalacionId,
        String instalacionNombre,
        Long clienteId,
        String tag,
        Long tipoEquipoId,
        String tipoEquipoNombre,
        String clasificacion,
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
