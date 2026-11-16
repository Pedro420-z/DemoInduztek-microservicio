package com.induztek.usuarios.service;

public interface NotificadorCorreo {

    void enviarContrasenaTemporal(String correo, String nombre, String contrasenaTemporal);
}
