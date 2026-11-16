package com.induztek.usuarios.service;

import com.induztek.usuarios.dto.ActualizarPerfilRequest;
import com.induztek.usuarios.dto.CambiarContrasenaRequest;
import com.induztek.usuarios.dto.ConfirmarRecuperacionRequest;
import com.induztek.usuarios.dto.CrearUsuarioRequest;
import com.induztek.usuarios.dto.LoginRequest;
import com.induztek.usuarios.dto.RecuperacionResponse;
import com.induztek.usuarios.dto.UsuarioResponse;
import com.induztek.usuarios.model.Rol;
import com.induztek.usuarios.model.Usuario;
import com.induztek.usuarios.repository.RolRepository;
import com.induztek.usuarios.repository.UsuarioRepository;
import com.induztek.usuarios.util.GeneradorCodigo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String CLAVE = "secreto1";
    private static final String TEMPORAL = "Temporal23x";
    private static final String RUT_VALIDO = "14.226.791-8";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private GeneradorCodigo generadorCodigo;

    @Mock
    private NotificadorCorreo notificador;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);

    private UsuarioService servicio;

    private final Rol rolTecnico = Rol.builder().id(1L).codigo("TECNICO").nombre("Técnico").build();
    private final Rol rolJefe = Rol.builder().id(3L).codigo("JEFE_TECNICO").nombre("Jefe Técnico").build();
    private final Rol rolCliente = Rol.builder().id(4L).codigo("CLIENTE").nombre("Cliente").build();

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarioRepository, rolRepository, encoder, generadorCodigo, reloj, notificador);
    }

    private Usuario usuario(Long id, Rol rol, boolean activo) {
        return Usuario.builder()
                .id(id)
                .rol(rol)
                .rut("11111111-1")
                .nombre("Usuario " + id)
                .correo("usuario" + id + "@induztek.cl")
                .passwordHash(encoder.encode(CLAVE))
                .activo(activo)
                .build();
    }

    private CrearUsuarioRequest peticion(Long rolId, String rut, String correo) {
        return new CrearUsuarioRequest(rut, "Ana Torres", correo, " 987654321 ", rolId,
                "Alimentos", "Marcela Fuentes", "Jefa de Mantenimiento");
    }

    private void assertEstado(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado.value(), error.getStatusCode().value());
    }

    private void actorJefe() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, rolJefe, true)));
    }

    @Test
    void listar_sinFiltro_devuelveTodosOrdenadosPorNombre() {
        when(usuarioRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(usuario(1L, rolJefe, true), usuario(2L, rolTecnico, true)));

        List<UsuarioResponse> lista = servicio.listar(null, null);

        assertEquals(2, lista.size());
    }

    @Test
    void listar_conTexto_buscaPorNombreOCorreoSinEspacios() {
        when(usuarioRepository.findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrderByNombreAsc("ana", "ana"))
                .thenReturn(List.of(usuario(2L, rolTecnico, true)));

        List<UsuarioResponse> lista = servicio.listar("  ana ", null);

        assertEquals(1, lista.size());
    }

    @Test
    void listar_conRol_filtraSoloEseRol() {
        when(usuarioRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(usuario(1L, rolJefe, true), usuario(2L, rolTecnico, true)));

        List<UsuarioResponse> lista = servicio.listar("", 1L);

        assertEquals(1, lista.size());
        assertEquals("TECNICO", lista.get(0).rolCodigo());
    }

    @Test
    void obtener_conIdExistente_devuelveElUsuario() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));

        UsuarioResponse respuesta = servicio.obtener(2L);

        assertEquals(2L, respuesta.id());
        assertEquals("Técnico", respuesta.rolNombre());
    }

    @Test
    void obtener_conIdInexistente_lanza404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.obtener(99L));
    }

    @Test
    void crear_jefeCreaPersonal_quedaActivoConClaveEncriptada() {
        when(generadorCodigo.generarContrasenaTemporal()).thenReturn(TEMPORAL);
        actorJefe();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolTecnico));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse respuesta = servicio.crear(1L, peticion(1L, RUT_VALIDO, "  ANA@Induztek.CL "));

        assertTrue(respuesta.activo());
        assertEquals("14226791-8", respuesta.rut());
        assertEquals("ana@induztek.cl", respuesta.correo());
        assertEquals("987654321", respuesta.telefono());
        assertNull(respuesta.giro());
    }

    @Test
    void crear_guardaLaClaveSoloComoHash() {
        when(generadorCodigo.generarContrasenaTemporal()).thenReturn(TEMPORAL);
        actorJefe();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolTecnico));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        servicio.crear(1L, peticion(1L, RUT_VALIDO, "ana@induztek.cl"));

        org.mockito.ArgumentCaptor<Usuario> guardado = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertNotEquals(TEMPORAL, guardado.getValue().getPasswordHash());
        assertTrue(encoder.matches(TEMPORAL, guardado.getValue().getPasswordHash()));
        assertTrue(guardado.getValue().isDebeCambiarContrasena());
    }

    @Test
    void crear_jefeCreaCliente_guardaLosDatosDeLaEmpresa() {
        when(generadorCodigo.generarContrasenaTemporal()).thenReturn(TEMPORAL);
        actorJefe();
        when(rolRepository.findById(4L)).thenReturn(Optional.of(rolCliente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse respuesta = servicio.crear(1L, peticion(4L, RUT_VALIDO, "cliente@empresa.cl"));

        assertTrue(respuesta.activo());
        assertEquals("Alimentos", respuesta.giro());
        assertEquals("Marcela Fuentes", respuesta.contactoNombre());
    }

    @Test
    void crear_tecnicoCreaCliente_quedaInactivoHastaQueElJefeLoHabilite() {
        when(generadorCodigo.generarContrasenaTemporal()).thenReturn(TEMPORAL);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));
        when(rolRepository.findById(4L)).thenReturn(Optional.of(rolCliente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse respuesta = servicio.crear(2L, peticion(4L, RUT_VALIDO, "cliente@empresa.cl"));

        assertFalse(respuesta.activo());
    }

    @Test
    void crear_tecnicoCreaPersonal_lanza403() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolTecnico));

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.crear(2L, peticion(1L, RUT_VALIDO, "a@b.cl")));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void crear_conActorCliente_lanza403() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario(5L, rolCliente, true)));

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.crear(5L, peticion(4L, RUT_VALIDO, "a@b.cl")));
    }

    @Test
    void crear_conActorInactivo_lanza403() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolJefe, false)));

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.crear(2L, peticion(1L, RUT_VALIDO, "a@b.cl")));
    }

    @Test
    void crear_sinIndicarActor_lanza401() {
        assertEstado(HttpStatus.UNAUTHORIZED, () -> servicio.crear(null, peticion(1L, RUT_VALIDO, "a@b.cl")));
    }

    @Test
    void crear_conActorInexistente_lanza401() {
        when(usuarioRepository.findById(77L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.UNAUTHORIZED, () -> servicio.crear(77L, peticion(1L, RUT_VALIDO, "a@b.cl")));
    }

    @Test
    void crear_sinRol_lanza400() {
        actorJefe();

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticion(null, RUT_VALIDO, "a@b.cl")));
    }

    @Test
    void crear_conRolInexistente_lanza400() {
        actorJefe();
        when(rolRepository.findById(9L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticion(9L, RUT_VALIDO, "a@b.cl")));
    }

    @Test
    void crear_conRutInvalido_lanza400() {
        actorJefe();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolTecnico));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticion(1L, "14.226.791-9", "a@b.cl")));
    }

    @Test
    void crear_enviaLaContrasenaTemporalPorCorreoYNoLaDevuelve() {
        when(generadorCodigo.generarContrasenaTemporal()).thenReturn(TEMPORAL);
        actorJefe();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolTecnico));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse respuesta = servicio.crear(1L, peticion(1L, RUT_VALIDO, "ana@induztek.cl"));

        verify(notificador).enviarContrasenaTemporal("ana@induztek.cl", "Ana Torres", TEMPORAL);
        assertTrue(respuesta.debeCambiarContrasena());
    }

    @Test
    void crear_conErrorDeValidacion_noEnviaCorreo() {
        actorJefe();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolTecnico));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticion(1L, RUT_VALIDO, "sin-arroba")));

        verify(notificador, never()).enviarContrasenaTemporal(any(), any(), any());
    }

    @Test
    void crear_conCorreoInvalido_lanza400() {
        actorJefe();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolTecnico));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticion(1L, RUT_VALIDO, "sin-arroba")));
    }

    @Test
    void crear_conNombreVacio_lanza400() {
        actorJefe();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolTecnico));
        CrearUsuarioRequest datos = new CrearUsuarioRequest(RUT_VALIDO, "  ", "a@b.cl", null, 1L, null, null, null);

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, datos));
    }

    @Test
    void crear_conRutRepetido_lanza409() {
        actorJefe();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolTecnico));
        when(usuarioRepository.existsByRut("14226791-8")).thenReturn(true);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.crear(1L, peticion(1L, RUT_VALIDO, "a@b.cl")));
    }

    @Test
    void crear_conCorreoRepetido_lanza409() {
        actorJefe();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolTecnico));
        when(usuarioRepository.existsByCorreo("a@b.cl")).thenReturn(true);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.crear(1L, peticion(1L, RUT_VALIDO, "a@b.cl")));
    }

    @Test
    void iniciarSesion_conDatosCorrectos_devuelveElUsuario() {
        when(usuarioRepository.findByCorreo("usuario2@induztek.cl")).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));

        UsuarioResponse respuesta = servicio.iniciarSesion(new LoginRequest(" Usuario2@Induztek.cl ", CLAVE));

        assertEquals(2L, respuesta.id());
    }

    @Test
    void iniciarSesion_conClaveIncorrecta_lanza401() {
        when(usuarioRepository.findByCorreo("usuario2@induztek.cl")).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));

        assertEstado(HttpStatus.UNAUTHORIZED,
                () -> servicio.iniciarSesion(new LoginRequest("usuario2@induztek.cl", "otraClave")));
    }

    @Test
    void iniciarSesion_sinClave_lanza401() {
        when(usuarioRepository.findByCorreo("usuario2@induztek.cl")).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));

        assertEstado(HttpStatus.UNAUTHORIZED,
                () -> servicio.iniciarSesion(new LoginRequest("usuario2@induztek.cl", null)));
    }

    @Test
    void iniciarSesion_conCorreoInexistente_lanza401() {
        when(usuarioRepository.findByCorreo("nadie@induztek.cl")).thenReturn(Optional.empty());

        assertEstado(HttpStatus.UNAUTHORIZED, () -> servicio.iniciarSesion(new LoginRequest("nadie@induztek.cl", CLAVE)));
    }

    @Test
    void iniciarSesion_sinCorreo_lanza401() {
        when(usuarioRepository.findByCorreo("")).thenReturn(Optional.empty());

        assertEstado(HttpStatus.UNAUTHORIZED, () -> servicio.iniciarSesion(new LoginRequest(null, CLAVE)));
    }

    @Test
    void iniciarSesion_conCuentaInactiva_lanza403() {
        when(usuarioRepository.findByCorreo("usuario2@induztek.cl")).thenReturn(Optional.of(usuario(2L, rolTecnico, false)));

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.iniciarSesion(new LoginRequest("usuario2@induztek.cl", CLAVE)));
    }

    @Test
    void actualizarPerfil_deUnTecnico_cambiaDatosPersonalesYNoLosDeEmpresa() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));
        when(usuarioRepository.existsByCorreoAndIdNot("nuevo@induztek.cl", 2L)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse respuesta = servicio.actualizarPerfil(2L,
                new ActualizarPerfilRequest("Nombre Nuevo", "Nuevo@Induztek.cl", "  ", "Giro", "Contacto", "Cargo"));

        assertEquals("Nombre Nuevo", respuesta.nombre());
        assertEquals("nuevo@induztek.cl", respuesta.correo());
        assertNull(respuesta.telefono());
        assertNull(respuesta.giro());
    }

    @Test
    void actualizarPerfil_deUnCliente_actualizaTambienLosDatosDeEmpresa() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario(5L, rolCliente, true)));
        when(usuarioRepository.existsByCorreoAndIdNot("c@empresa.cl", 5L)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse respuesta = servicio.actualizarPerfil(5L,
                new ActualizarPerfilRequest("Cliente", "c@empresa.cl", "123456789", "Minería", "Pedro", "Supervisor"));

        assertEquals("Minería", respuesta.giro());
        assertEquals("Pedro", respuesta.contactoNombre());
        assertEquals("Supervisor", respuesta.contactoCargo());
    }

    @Test
    void actualizarPerfil_conCorreoDeOtroUsuario_lanza409() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));
        when(usuarioRepository.existsByCorreoAndIdNot("otro@induztek.cl", 2L)).thenReturn(true);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.actualizarPerfil(2L,
                new ActualizarPerfilRequest("Nombre", "otro@induztek.cl", null, null, null, null)));
    }

    @Test
    void actualizarPerfil_conNombreVacio_lanza400() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.actualizarPerfil(2L,
                new ActualizarPerfilRequest("", "a@b.cl", null, null, null, null)));
    }

    @Test
    void cambiarContrasena_conClaveActualCorrecta_guardaElNuevoHash() {
        Usuario existente = usuario(2L, rolTecnico, true);
        existente.setDebeCambiarContrasena(true);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(existente));

        servicio.cambiarContrasena(2L, new CambiarContrasenaRequest(CLAVE, "nuevaClave9"));

        assertTrue(encoder.matches("nuevaClave9", existente.getPasswordHash()));
        assertFalse(existente.isDebeCambiarContrasena());
        verify(usuarioRepository).save(existente);
    }

    @Test
    void cambiarContrasena_conClaveActualIncorrecta_lanza401() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));

        assertEstado(HttpStatus.UNAUTHORIZED,
                () -> servicio.cambiarContrasena(2L, new CambiarContrasenaRequest("mala", "nuevaClave9")));
    }

    @Test
    void cambiarContrasena_sinClaveActual_lanza401() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));

        assertEstado(HttpStatus.UNAUTHORIZED,
                () -> servicio.cambiarContrasena(2L, new CambiarContrasenaRequest(null, "nuevaClave9")));
    }

    @Test
    void cambiarContrasena_conNuevaClaveCorta_lanza400() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));

        assertEstado(HttpStatus.BAD_REQUEST,
                () -> servicio.cambiarContrasena(2L, new CambiarContrasenaRequest(CLAVE, "123")));
    }

    @Test
    void cambiarContrasena_conLaMismaClave_lanza400() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));

        assertEstado(HttpStatus.BAD_REQUEST,
                () -> servicio.cambiarContrasena(2L, new CambiarContrasenaRequest(CLAVE, CLAVE)));
    }

    @Test
    void cambiarEstado_jefeDesactivaAOtroUsuario() {
        actorJefe();
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse respuesta = servicio.cambiarEstado(1L, 2L, false);

        assertFalse(respuesta.activo());
    }

    @Test
    void cambiarEstado_jefeHabilitaUnaCuentaInactiva() {
        actorJefe();
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario(5L, rolCliente, false)));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse respuesta = servicio.cambiarEstado(1L, 5L, true);

        assertTrue(respuesta.activo());
    }

    @Test
    void cambiarEstado_quienNoEsJefe_lanza403() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.cambiarEstado(2L, 3L, false));
    }

    @Test
    void cambiarEstado_sinIndicarEstado_lanza400() {
        actorJefe();

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.cambiarEstado(1L, 2L, null));
    }

    @Test
    void cambiarEstado_jefeNoPuedeDesactivarseASiMismo() {
        actorJefe();

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.cambiarEstado(1L, 1L, false));
    }

    @Test
    void solicitarRecuperacion_guardaElCodigoConVigenciaDeQuinceMinutos() {
        Usuario existente = usuario(2L, rolTecnico, true);
        when(usuarioRepository.findByCorreo("usuario2@induztek.cl")).thenReturn(Optional.of(existente));
        when(generadorCodigo.generar()).thenReturn("123456");

        RecuperacionResponse respuesta = servicio.solicitarRecuperacion(" Usuario2@induztek.cl ");

        assertEquals("123456", respuesta.codigo());
        assertEquals(LocalDateTime.of(2026, 11, 15, 12, 15), respuesta.expiraEn());
        assertEquals("123456", existente.getCodigoRecuperacion());
        verify(usuarioRepository).save(existente);
    }

    @Test
    void solicitarRecuperacion_conCorreoInexistente_lanza404() {
        when(usuarioRepository.findByCorreo("nadie@induztek.cl")).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.solicitarRecuperacion("nadie@induztek.cl"));
    }

    @Test
    void solicitarRecuperacion_sinCorreo_lanza404() {
        when(usuarioRepository.findByCorreo("")).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.solicitarRecuperacion(null));
    }

    @Test
    void solicitarRecuperacion_conCuentaInactiva_lanza403() {
        when(usuarioRepository.findByCorreo("usuario2@induztek.cl")).thenReturn(Optional.of(usuario(2L, rolTecnico, false)));

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.solicitarRecuperacion("usuario2@induztek.cl"));
    }

    @Test
    void confirmarRecuperacion_conCodigoVigente_cambiaLaClaveYBorraElCodigo() {
        Usuario existente = usuario(2L, rolTecnico, true);
        existente.setDebeCambiarContrasena(true);
        existente.setCodigoRecuperacion("123456");
        existente.setRecuperacionExpira(LocalDateTime.of(2026, 11, 15, 12, 10));
        when(usuarioRepository.findByCorreo("usuario2@induztek.cl")).thenReturn(Optional.of(existente));

        servicio.confirmarRecuperacion(new ConfirmarRecuperacionRequest("usuario2@induztek.cl", "123456", "claveNueva1"));

        assertTrue(encoder.matches("claveNueva1", existente.getPasswordHash()));
        assertNull(existente.getCodigoRecuperacion());
        assertNull(existente.getRecuperacionExpira());
        assertFalse(existente.isDebeCambiarContrasena());
        verify(usuarioRepository).save(existente);
    }

    @Test
    void confirmarRecuperacion_conCodigoIncorrecto_lanza400() {
        Usuario existente = usuario(2L, rolTecnico, true);
        existente.setCodigoRecuperacion("123456");
        existente.setRecuperacionExpira(LocalDateTime.of(2026, 11, 15, 12, 10));
        when(usuarioRepository.findByCorreo("usuario2@induztek.cl")).thenReturn(Optional.of(existente));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.confirmarRecuperacion(
                new ConfirmarRecuperacionRequest("usuario2@induztek.cl", "000000", "claveNueva1")));
    }

    @Test
    void confirmarRecuperacion_sinHaberSolicitadoCodigo_lanza400() {
        when(usuarioRepository.findByCorreo("usuario2@induztek.cl")).thenReturn(Optional.of(usuario(2L, rolTecnico, true)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.confirmarRecuperacion(
                new ConfirmarRecuperacionRequest("usuario2@induztek.cl", "123456", "claveNueva1")));
    }

    @Test
    void confirmarRecuperacion_conCodigoExpirado_lanza400() {
        Usuario existente = usuario(2L, rolTecnico, true);
        existente.setCodigoRecuperacion("123456");
        existente.setRecuperacionExpira(LocalDateTime.of(2026, 11, 15, 11, 0));
        when(usuarioRepository.findByCorreo("usuario2@induztek.cl")).thenReturn(Optional.of(existente));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.confirmarRecuperacion(
                new ConfirmarRecuperacionRequest("usuario2@induztek.cl", "123456", "claveNueva1")));
    }

    @Test
    void confirmarRecuperacion_conClaveNuevaCorta_lanza400() {
        Usuario existente = usuario(2L, rolTecnico, true);
        existente.setCodigoRecuperacion("123456");
        existente.setRecuperacionExpira(LocalDateTime.of(2026, 11, 15, 12, 10));
        when(usuarioRepository.findByCorreo("usuario2@induztek.cl")).thenReturn(Optional.of(existente));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.confirmarRecuperacion(
                new ConfirmarRecuperacionRequest("usuario2@induztek.cl", "123456", "123")));
    }
}
