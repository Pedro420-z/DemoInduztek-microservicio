package com.induztek.protocolos.dto;

import java.math.BigDecimal;

public record MedicionResponse(
        String parametroCodigo,
        String parametroNombre,
        String unidad,
        BigDecimal valor
) {
}
