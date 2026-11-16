package com.induztek.protocolos.dto;

import com.induztek.protocolos.model.TipoVisita;

import java.time.LocalDate;
import java.util.List;

public record VisitaRequest(
        String numeroOt,
        Long instalacionId,
        LocalDate fecha,
        TipoVisita tipoVisita,
        Long ingenieroId,
        List<Long> participantesIds
) {
}
