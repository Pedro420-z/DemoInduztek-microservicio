package com.induztek.equipos.service;

import com.induztek.equipos.client.UsuariosCliente;
import com.induztek.equipos.dto.UsuarioExterno;
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
    private ActorService servicio;

    private UsuarioExterno usuario(Long id, String rol, boolean activo) {
        return new UsuarioExterno(id, 1L, rol, rol, "11111111-1", "Usuario " + id,
                "u" + id + "@induztek.cl", null, activo, null, null, null, false);
    }

    private void assertEstado(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado.value(), error.getStatusCode().value());
    }

    @Test
    void exigirJefeTecnico_sinActor_lanza401() {
        assertEstado(HttpStatus.UNAUTHORIZED, () -> servicio.exigirJefeTecnico(null));
    }

    @Test
    void exigirJefeTecnico_conActorInexistente_lanza401() {
        when(usuariosCliente.buscar(9L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.UNAUTHORIZED, () -> servicio.exigirJefeTecnico(9L));
    }

    @Test
    void exigirJefeTecnico_conCuentaInactiva_lanza403() {
        when(usuariosCliente.buscar(1L)).thenReturn(Optional.of(usuario(1L, "JEFE_TECNICO", false)));

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.exigirJefeTecnico(1L));
    }

    @Test
    void exigirJefeTecnico_conOtroRol_lanza403() {
        when(usuariosCliente.buscar(2L)).thenReturn(Optional.of(usuario(2L, "INGENIERO", true)));

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.exigirJefeTecnico(2L));
    }

    @Test
    void exigirJefeTecnico_conJefeActivo_devuelveAlUsuario() {
        when(usuariosCliente.buscar(1L)).thenReturn(Optional.of(usuario(1L, "JEFE_TECNICO", true)));

        assertEquals(1L, servicio.exigirJefeTecnico(1L).id());
    }
}
