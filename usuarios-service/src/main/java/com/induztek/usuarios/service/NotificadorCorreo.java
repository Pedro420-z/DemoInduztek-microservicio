package com.induztek.usuarios.service;

import java.time.LocalDateTime;

public interface NotificadorCorreo {

    void enviarContrasenaTemporal(String correo, String nombre, String contrasenaTemporal);

    void enviarCodigoRecuperacion(String correo, String nombre, String codigo, LocalDateTime expira);
}
