package com.induztek.protocolos.dto;

public record RecepcionRequest(
        String receptorNombre,
        String receptorRut,
        String receptorCargo
) {
}
