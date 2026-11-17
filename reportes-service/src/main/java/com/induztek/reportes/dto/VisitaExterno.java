package com.induztek.reportes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record VisitaExterno(
        Long id,
        String numeroOt,
        Long instalacionId,
        LocalDate fecha,
        String tipoVisita,
        Long ingenieroId,
        List<Long> participantesIds,
        LocalDateTime fechaHoraCierre,
        boolean cerrada,
        RecepcionExterno recepcion
) {
}
