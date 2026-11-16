package com.induztek.protocolos.dto;

import java.time.LocalDateTime;

public record RecepcionResponse(
        String receptorNombre,
        String receptorRut,
        String receptorCargo,
        LocalDateTime fechaHoraRecepcion
) {
}
