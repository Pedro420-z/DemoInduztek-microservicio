package com.induztek.reportes.dto;

import java.util.List;

public record ActaResponse(
        VisitaExterno visita,
        String instalacionNombre,
        List<ProtocoloExterno> protocolos
) {
}
