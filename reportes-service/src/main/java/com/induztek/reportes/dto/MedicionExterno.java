package com.induztek.reportes.dto;

import java.math.BigDecimal;

public record MedicionExterno(
        String parametroCodigo,
        String parametroNombre,
        String unidad,
        BigDecimal valor
) {
}
