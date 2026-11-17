package com.induztek.reportes.dto;

import java.time.LocalDateTime;

public record RecepcionExterno(
        String receptorNombre,
        String receptorRut,
        String receptorCargo,
        LocalDateTime fechaHoraRecepcion
) {
}
