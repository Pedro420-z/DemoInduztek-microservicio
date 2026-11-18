package com.induztek.usuarios.dto;

import java.time.LocalDateTime;

public record RecuperacionResponse(String mensaje, LocalDateTime expiraEn) {
}
