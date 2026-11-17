package com.induztek.protocolos.service;

import com.induztek.protocolos.client.UsuariosCliente;
import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.util.Roles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActorServiceRolTest {

    @Mock
    private UsuariosCliente usuariosCliente;

    @InjectMocks
    private ActorService actorService;

    private static UsuarioExterno usuario(String rol) {
        return new UsuarioExterno(3L, 1L, rol, rol, "1-9", "Nombre", "a@b.cl", null, true, null, null, null, false);
    }

    @Test
    void exigirRol_conRolPermitido_devuelveAlUsuario() {
        when(usuariosCliente.buscar(3L)).thenReturn(Optional.of(usuario(Roles.INGENIERO)));

        UsuarioExterno actor = actorService.exigirRol(3L, "No permitido", Roles.INGENIERO, Roles.JEFE_TECNICO);

        assertEquals(Roles.INGENIERO, actor.rolCodigo());
    }

    @Test
    void exigirRol_conOtroRol_lanza403ConElMensaje() {
        when(usuariosCliente.buscar(3L)).thenReturn(Optional.of(usuario(Roles.TECNICO)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> actorService.exigirRol(3L, "No permitido", Roles.INGENIERO, Roles.JEFE_TECNICO));

        assertEquals(HttpStatus.FORBIDDEN.value(), error.getStatusCode().value());
        assertEquals("No permitido", error.getReason());
    }
}
