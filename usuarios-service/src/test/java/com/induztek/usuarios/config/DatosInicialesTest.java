package com.induztek.usuarios.config;

import com.induztek.usuarios.model.Rol;
import com.induztek.usuarios.model.Usuario;
import com.induztek.usuarios.repository.RolRepository;
import com.induztek.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatosInicialesTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<List<Rol>> rolesGuardados;

    private DatosIniciales datosIniciales;

    @BeforeEach
    void preparar() {
        datosIniciales = new DatosIniciales(rolRepository, usuarioRepository, passwordEncoder);
        ReflectionTestUtils.setField(datosIniciales, "jefeRut", "11111111-1");
        ReflectionTestUtils.setField(datosIniciales, "jefeNombre", "Jefe Inicial");
        ReflectionTestUtils.setField(datosIniciales, "jefeCorreo", "jefe@induztek.cl");
        ReflectionTestUtils.setField(datosIniciales, "jefeContrasena", "Induztek2026");
        ReflectionTestUtils.setField(datosIniciales, "clientesContrasena", "Induztek2026");
    }

    private Rol rol(Long id, String codigo) {
        return Rol.builder().id(id).codigo(codigo).nombre(codigo).build();
    }

    @Test
    void run_conBaseVacia_cargaLosCuatroRolesYElJefeInicial() {
        when(rolRepository.count()).thenReturn(0L);
        when(usuarioRepository.count()).thenReturn(0L);
        when(rolRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                rol(1L, DatosIniciales.TECNICO),
                rol(2L, DatosIniciales.INGENIERO),
                rol(3L, DatosIniciales.JEFE_TECNICO),
                rol(4L, DatosIniciales.CLIENTE)));
        when(passwordEncoder.encode("Induztek2026")).thenReturn("hash");

        datosIniciales.run();

        verify(rolRepository).saveAll(rolesGuardados.capture());
        assertEquals(4, rolesGuardados.getValue().size());
        assertEquals("TECNICO", rolesGuardados.getValue().get(0).getCodigo());

        ArgumentCaptor<Usuario> guardados = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(5)).save(guardados.capture());
        Usuario jefe = guardados.getAllValues().get(0);
        assertEquals("jefe@induztek.cl", jefe.getCorreo());
        assertEquals("hash", jefe.getPasswordHash());
        assertEquals("JEFE_TECNICO", jefe.getRol().getCodigo());
        assertTrue(jefe.isActivo());
        assertTrue(jefe.isDebeCambiarContrasena());
        assertEquals("INGENIERO", guardados.getAllValues().get(1).getRol().getCodigo());
        assertEquals("TECNICO", guardados.getAllValues().get(2).getRol().getCodigo());
        assertEquals("CLIENTE", guardados.getAllValues().get(3).getRol().getCodigo());
        assertEquals("CLIENTE", guardados.getAllValues().get(4).getRol().getCodigo());
    }

    @Test
    void run_conDatosExistentes_noVuelveACargarNada() {
        when(rolRepository.count()).thenReturn(4L);
        when(usuarioRepository.count()).thenReturn(1L);
        when(rolRepository.findAllByOrderByIdAsc()).thenReturn(List.of(rol(4L, DatosIniciales.CLIENTE)));
        when(usuarioRepository.existsByCorreo(any())).thenReturn(true);

        datosIniciales.run();

        verify(rolRepository, never()).saveAll(anyList());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void run_siNoExisteElRolJefe_lanzaExcepcion() {
        when(rolRepository.count()).thenReturn(4L);
        when(usuarioRepository.count()).thenReturn(0L);
        when(rolRepository.findAllByOrderByIdAsc()).thenReturn(List.of(rol(1L, DatosIniciales.TECNICO)));

        assertThrows(NoSuchElementException.class, () -> datosIniciales.run());
    }
}
