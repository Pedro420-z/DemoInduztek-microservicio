package com.induztek.protocolos.dto;

import com.induztek.protocolos.model.TipoVisita;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record VisitaResponse(
        Long id,
        String numeroOt,
        Long instalacionId,
        LocalDate fecha,
        TipoVisita tipoVisita,
        Long ingenieroId,
        List<Long> participantesIds,
        LocalDateTime fechaHoraCierre,
        boolean cerrada,
        RecepcionResponse recepcion
) {
}
