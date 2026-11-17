package com.induztek.reportes.dto;

import java.util.List;
import java.util.Map;

public record HistorialEquipoResponse(
        EquipoExterno equipo,
        int totalProtocolos,
        Map<String, Long> porResultado,
        List<ProtocoloExterno> protocolos
) {
}
