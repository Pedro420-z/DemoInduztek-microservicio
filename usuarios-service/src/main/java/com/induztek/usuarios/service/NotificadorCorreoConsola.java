package com.induztek.usuarios.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class NotificadorCorreoConsola implements NotificadorCorreo {

    @Override
    public void enviarContrasenaTemporal(String correo, String nombre, String contrasenaTemporal) {
        log.info("[CORREO SIMULADO] Para: {} | Hola {}, tu contraseña temporal es: {} | Debes cambiarla al ingresar por primera vez.",
                correo, nombre, contrasenaTemporal);
    }

    @Override
    public void enviarCodigoRecuperacion(String correo, String nombre, String codigo, LocalDateTime expira) {
        log.info("[CORREO SIMULADO] Para: {} | Hola {}, tu código de recuperación es: {} | Vence a las {}.",
                correo, nombre, codigo, expira);
    }
}
