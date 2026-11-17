package com.induztek.protocolos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProtocoloActualizacionRequest(
        LocalDateTime fechaHoraPrueba,
        BigDecimal temperaturaC,
        BigDecimal humedadPct,
        String descripcionMantenimiento,
        String observaciones
) {
}
