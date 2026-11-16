package com.induztek.usuarios.dto;

import java.time.LocalDateTime;

public record RecuperacionResponse(String codigo, LocalDateTime expiraEn) {
}
