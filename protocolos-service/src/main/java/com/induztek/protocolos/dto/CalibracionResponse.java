package com.induztek.protocolos.dto;

import java.time.LocalDate;

public record CalibracionResponse(
        Long id,
        LocalDate fechaCalibracion,
        LocalDate fechaVencimiento
) {
}
