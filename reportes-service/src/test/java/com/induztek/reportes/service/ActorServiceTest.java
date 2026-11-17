package com.induztek.reportes.service;

import com.induztek.reportes.client.UsuariosCliente;
import com.induztek.reportes.dto.UsuarioExterno;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
class ActorServiceTest {

    @Mock
    private UsuariosCliente usuariosCliente;

    @InjectMocks
    private ActorService actorService;

    private static UsuarioExterno usuario(String rol, boolean activo) {
        return new UsuarioExterno(3L, 1L, rol, rol, "1-9", "Nombre", "a@b.cl", null, activo, null, null, null, false);
    }

    private void assertEstado(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado.value(), error.getStatusCode().value());
    }

    @Test
    void exigirUsuarioActivo_sinIdentificarse_lanza401() {
        assertEstado(HttpStatus.UNAUTHORIZED, () -> actorService.exigirUsuarioActivo(null));
    }

    @Test
    void exigirUsuarioActivo_conUsuarioInexistente_lanza401() {
        when(usuariosCliente.buscar(3L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.UNAUTHORIZED, () -> actorService.exigirUsuarioActivo(3L));
    }

    @Test
    void exigirUsuarioActivo_conCuentaInactiva_lanza403() {
        when(usuariosCliente.buscar(3L)).thenReturn(Optional.of(usuario("CLIENTE", false)));

        assertEstado(HttpStatus.FORBIDDEN, () -> actorService.exigirUsuarioActivo(3L));
    }

    @Test
    void exigirUsuarioActivo_aceptaAClientesActivos() {
        when(usuariosCliente.buscar(3L)).thenReturn(Optional.of(usuario("CLIENTE", true)));

        assertEquals("CLIENTE", actorService.exigirUsuarioActivo(3L).rolCodigo());
    }

    @Test
    void exigirPersonalActivo_conCliente_lanza403() {
        when(usuariosCliente.buscar(3L)).thenReturn(Optional.of(usuario("CLIENTE", true)));

        assertEstado(HttpStatus.FORBIDDEN, () -> actorService.exigirPersonalActivo(3L));
    }

    @Test
    void exigirPersonalActivo_conTecnico_loDevuelve() {
        when(usuariosCliente.buscar(3L)).thenReturn(Optional.of(usuario("TECNICO", true)));

        assertEquals("TECNICO", actorService.exigirPersonalActivo(3L).rolCodigo());
    }
}
