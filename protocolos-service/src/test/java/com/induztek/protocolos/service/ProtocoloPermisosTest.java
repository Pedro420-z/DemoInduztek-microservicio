package com.induztek.protocolos.service;

import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.util.Roles;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtocoloPermisosTest {

    private final ProtocoloPermisos permisos = new ProtocoloPermisos();

    private static UsuarioExterno usuario(Long id, String rol) {
        return new UsuarioExterno(id, 1L, rol, rol, "1-9", "Nombre", "a@b.cl", null, true, null, null, null, false);
    }

    private static Protocolo protocolo(EstadoProtocolo estado, Long registraId) {
        return Protocolo.builder().id(1L).estado(estado).usuarioRegistraId(registraId).build();
    }

    @Test
    void tecnicoPuedeEditarSuPropioBorrador() {
        assertDoesNotThrow(() -> permisos.exigirEditable(usuario(5L, Roles.TECNICO), protocolo(EstadoProtocolo.BORRADOR, 5L)));
    }

    @Test
    void tecnicoPuedeEditarSuPropioProtocoloObservado() {
        assertDoesNotThrow(() -> permisos.exigirEditable(usuario(5L, Roles.TECNICO), protocolo(EstadoProtocolo.OBSERVADO, 5L)));
    }

    @Test
    void tecnicoNoPuedeEditarProtocoloAjeno() {
        UsuarioExterno tecnico = usuario(5L, Roles.TECNICO);
        Protocolo ajeno = protocolo(EstadoProtocolo.BORRADOR, 6L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> permisos.exigirEditable(tecnico, ajeno));

        assertEquals(HttpStatus.FORBIDDEN.value(), error.getStatusCode().value());
    }

    @Test
    void ingenieroPuedeEditarProtocoloDeOtro() {
        assertDoesNotThrow(() -> permisos.exigirEditable(usuario(9L, Roles.INGENIERO), protocolo(EstadoProtocolo.BORRADOR, 6L)));
    }

    @Test
    void protocoloRegistradoNoSePuedeEditar() {
        UsuarioExterno ingeniero = usuario(9L, Roles.INGENIERO);
        Protocolo registrado = protocolo(EstadoProtocolo.REGISTRADO_TERRENO, 9L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> permisos.exigirEditable(ingeniero, registrado));

        assertEquals(HttpStatus.CONFLICT.value(), error.getStatusCode().value());
    }
}
