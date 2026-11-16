package com.induztek.protocolos.dto;

import java.time.LocalDate;

public record CalibracionRequest(
        LocalDate fechaCalibracion,
        LocalDate fechaVencimiento
) {
}
