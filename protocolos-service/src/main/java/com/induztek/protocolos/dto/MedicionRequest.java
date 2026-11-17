package com.induztek.protocolos.dto;

import java.math.BigDecimal;

public record MedicionRequest(
        String parametro,
        BigDecimal valor
) {
}
